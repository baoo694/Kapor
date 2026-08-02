from dataclasses import replace
from fastapi import FastAPI, File, Form, HTTPException, UploadFile
from pydantic import BaseModel
from typing import List, Optional
from pathlib import Path
from tempfile import NamedTemporaryFile
from threading import Lock
import logging
import os

app = FastAPI(title="Kapor NLP Service")
logger = logging.getLogger(__name__)

# WhisperX is loaded on pronunciation request. Its ASR and Korean forced
# alignment models are cached in the Docker volume.
_whisperx_model = None
_whisperx_align_model = None
_whisperx_align_metadata = None
_whisper_lock = Lock()
# WhisperX keeps decoding options on the cached model. Serialise only the
# decoding step so each scripted exercise receives its own initial prompt.
_whisper_inference_lock = Lock()
WHISPERX_MODEL_NAME = os.getenv("WHISPERX_MODEL", os.getenv("WHISPER_MODEL", "small"))
WHISPERX_DEVICE = os.getenv("WHISPERX_DEVICE", os.getenv("WHISPER_DEVICE", "cpu"))
WHISPERX_COMPUTE_TYPE = os.getenv("WHISPERX_COMPUTE_TYPE", os.getenv("WHISPER_COMPUTE_TYPE", "int8"))
# Silero avoids loading WhisperX's legacy Pyannote VAD checkpoint. This is
# materially lighter on the CPU-only development deployment.
WHISPERX_VAD_METHOD = os.getenv("WHISPERX_VAD_METHOD", "silero")
WHISPERX_BATCH_SIZE = int(os.getenv("WHISPERX_BATCH_SIZE", "1"))

class TranscribedWord(BaseModel):
    text: str
    startSeconds: Optional[float] = None
    endSeconds: Optional[float] = None
    probability: Optional[float] = None
    timingStatus: str = "unaligned"

class WhisperTranscriptionResponse(BaseModel):
    text: str
    durationSeconds: float
    words: List[TranscribedWord]

@app.get("/health")
def health_check():
    return {
        "status": "ok",
        "whisperx_model": WHISPERX_MODEL_NAME,
        "whisperx_vad_method": WHISPERX_VAD_METHOD,
        "whisperx_loaded": _whisperx_model is not None,
    }

def get_whisperx_models():
    global _whisperx_model, _whisperx_align_model, _whisperx_align_metadata
    if _whisperx_model is not None and _whisperx_align_model is not None:
        return _whisperx_model, _whisperx_align_model, _whisperx_align_metadata
    with _whisper_lock:
        if _whisperx_model is None or _whisperx_align_model is None:
            try:
                import whisperx
                _whisperx_model = whisperx.load_model(
                    WHISPERX_MODEL_NAME,
                    WHISPERX_DEVICE,
                    compute_type=WHISPERX_COMPUTE_TYPE,
                    language="ko",
                    vad_method=WHISPERX_VAD_METHOD,
                )
                _whisperx_align_model, _whisperx_align_metadata = whisperx.load_align_model(
                    language_code="ko", device=WHISPERX_DEVICE,
                )
            except Exception as error:
                logger.exception("Unable to load WhisperX Korean models '%s'", WHISPERX_MODEL_NAME)
                raise HTTPException(
                    status_code=503,
                    detail="WhisperX Korean models are unavailable. Check model download and runtime memory.",
                ) from error
    return _whisperx_model, _whisperx_align_model, _whisperx_align_metadata

@app.post("/pronunciation/transcribe", response_model=WhisperTranscriptionResponse)
async def transcribe_korean_pronunciation(
    audio: UploadFile = File(...),
    reference_text: str = Form(default="", alias="referenceText"),
):
    """WhisperX transcript and word timing only; Azure performs scoring in Spring."""
    if audio.content_type not in {"audio/wav", "audio/x-wav", "application/octet-stream"}:
        raise HTTPException(status_code=400, detail="Expected WAV audio")
    suffix = Path(audio.filename or "attempt.wav").suffix or ".wav"
    temp_path = None
    try:
        with NamedTemporaryFile(suffix=suffix, delete=False) as temp_file:
            temp_path = temp_file.name
            temp_file.write(await audio.read())

        import whisperx
        model, align_model, align_metadata = get_whisperx_models()
        audio_data = whisperx.load_audio(temp_path)
        # WhisperX 3.4 stores initial_prompt in the pipeline options rather
        # than exposing it on transcribe(). It biases decoding toward the
        # exercise sentence but does not force the returned transcript.
        with _whisper_inference_lock:
            original_options = model.options
            prompt = " ".join(reference_text.split())
            if prompt:
                model.options = replace(original_options, initial_prompt=prompt)
            try:
                result = model.transcribe(
                    audio_data,
                    language="ko",
                    batch_size=WHISPERX_BATCH_SIZE,
                )
            finally:
                model.options = original_options
        aligned = whisperx.align(
            result["segments"], align_model, align_metadata, audio_data,
            WHISPERX_DEVICE, return_char_alignments=False,
        )
        text_parts = [segment.get("text", "").strip() for segment in aligned.get("segments", [])]
        words = [timeline_word(word) for segment in aligned.get("segments", []) for word in segment.get("words", [])]
        return WhisperTranscriptionResponse(
            text=" ".join(part for part in text_parts if part).strip(),
            durationSeconds=round(len(audio_data) / 16000, 2),
            words=words,
        )
    except HTTPException:
        raise
    except Exception as error:
        logger.exception("WhisperX could not transcribe and align Korean recording")
        raise HTTPException(status_code=502, detail="WhisperX could not transcribe this recording") from error
    finally:
        if temp_path:
            Path(temp_path).unlink(missing_ok=True)


def timeline_word(word):
    start, end = word.get("start"), word.get("end")
    aligned = isinstance(start, (int, float)) and isinstance(end, (int, float))
    return TranscribedWord(
        text=str(word.get("word", "")).strip(),
        startSeconds=round(start, 2) if aligned else None,
        endSeconds=round(end, 2) if aligned else None,
        probability=round(word["score"], 3) if isinstance(word.get("score"), (int, float)) else None,
        timingStatus="aligned" if aligned else "unaligned",
    )

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8001)
