from fastapi import FastAPI, File, HTTPException, UploadFile
from pydantic import BaseModel
from typing import List, Optional
from pathlib import Path
from tempfile import NamedTemporaryFile
from threading import Lock
import logging
import os
import mecab

app = FastAPI(title="Kapor NLP Service")
logger = logging.getLogger(__name__)

# Whisper is intentionally loaded on the first pronunciation request so the
# existing tokenizer endpoints can start quickly. Model files are downloaded
# once by faster-whisper and then kept in the Docker volume/cache.
_whisper_model = None
_whisper_lock = Lock()
WHISPER_MODEL_NAME = os.getenv("WHISPER_MODEL", "small")
WHISPER_DEVICE = os.getenv("WHISPER_DEVICE", "cpu")
WHISPER_COMPUTE_TYPE = os.getenv("WHISPER_COMPUTE_TYPE", "int8")

# Initialize Mecab tokenizer for Korean
try:
    m = mecab.MeCab()
except Exception as e:
    print(f"Failed to initialize MeCab: {e}")
    m = None

class TokenizeRequest(BaseModel):
    text: str

class Token(BaseModel):
    surface: str
    stem: str
    pos: str
    isContentWord: bool

class TokenizeResponse(BaseModel):
    tokens: List[Token]

class TranscribedWord(BaseModel):
    text: str
    startSeconds: float
    endSeconds: float
    probability: Optional[float] = None

class WhisperTranscriptionResponse(BaseModel):
    text: str
    durationSeconds: float
    words: List[TranscribedWord]

CONTENT_POS_TAGS = {"NNG", "NNP", "VV", "VA", "MAG", "SL", "SH"}

@app.get("/health")
def health_check():
    return {
        "status": "ok",
        "mecab_initialized": m is not None,
        "whisper_model": WHISPER_MODEL_NAME,
        "whisper_loaded": _whisper_model is not None,
    }

@app.post("/tokenize", response_model=TokenizeResponse)
def tokenize_text(request: TokenizeRequest):
    if not m:
        raise HTTPException(status_code=500, detail="Mecab is not initialized")
    
    parsed_nodes = m.parse(request.text)
    
    tokens = []
    for node in parsed_nodes:
        # node structure depends on mecab-ko wrapper, usually: surface, feature (POS, semantic, etc.)
        surface = node[0]
        feature = node[1].split(',')
        pos = feature[0]
        
        # Stem extraction (if available, otherwise fallback to surface)
        stem = feature[7] if len(feature) > 7 and feature[7] != '*' else surface
        
        # Is content word?
        is_content_word = any(pos.startswith(tag) for tag in CONTENT_POS_TAGS)
        
        tokens.append(Token(
            surface=surface,
            stem=stem.split('/')[0], # Handle cases where stem has multiple parts separated by /
            pos=pos,
            isContentWord=is_content_word
        ))
        
    return TokenizeResponse(tokens=tokens)

def get_whisper_model():
    global _whisper_model
    if _whisper_model is not None:
        return _whisper_model
    with _whisper_lock:
        if _whisper_model is None:
            try:
                from faster_whisper import WhisperModel
                _whisper_model = WhisperModel(
                    WHISPER_MODEL_NAME,
                    device=WHISPER_DEVICE,
                    compute_type=WHISPER_COMPUTE_TYPE,
                )
            except Exception as error:
                logger.exception("Unable to load local Whisper model '%s'", WHISPER_MODEL_NAME)
                raise HTTPException(
                    status_code=503,
                    detail="Whisper local model is unavailable. Check model download and runtime memory.",
                ) from error
    return _whisper_model

@app.post("/pronunciation/transcribe", response_model=WhisperTranscriptionResponse)
async def transcribe_korean_pronunciation(audio: UploadFile = File(...)):
    """Offline Korean STT. Scoring stays in Spring so it is auditable and deterministic."""
    if audio.content_type not in {"audio/wav", "audio/x-wav", "application/octet-stream"}:
        raise HTTPException(status_code=400, detail="Expected WAV audio")
    suffix = Path(audio.filename or "attempt.wav").suffix or ".wav"
    temp_path = None
    try:
        with NamedTemporaryFile(suffix=suffix, delete=False) as temp_file:
            temp_path = temp_file.name
            temp_file.write(await audio.read())

        model = get_whisper_model()
        segments, info = model.transcribe(
            temp_path,
            language="ko",
            task="transcribe",
            beam_size=5,
            vad_filter=True,
            word_timestamps=True,
        )
        text_parts = []
        words = []
        for segment in segments:
            text_parts.append(segment.text.strip())
            for word in segment.words or []:
                words.append(TranscribedWord(
                    text=word.word.strip(),
                    startSeconds=round(word.start, 2),
                    endSeconds=round(word.end, 2),
                    probability=round(word.probability, 3) if word.probability is not None else None,
                ))
        return WhisperTranscriptionResponse(
            text=" ".join(part for part in text_parts if part).strip(),
            durationSeconds=round(info.duration, 2),
            words=words,
        )
    except HTTPException:
        raise
    except Exception as error:
        raise HTTPException(status_code=502, detail="Whisper could not transcribe this recording") from error
    finally:
        if temp_path:
            Path(temp_path).unlink(missing_ok=True)

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8001)
