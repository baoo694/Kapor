from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List
import mecab

app = FastAPI(title="Kapor NLP Service")

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

CONTENT_POS_TAGS = {"NNG", "NNP", "VV", "VA", "MAG", "SL", "SH"}

@app.get("/health")
def health_check():
    return {"status": "ok", "mecab_initialized": m is not None}

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

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8001)
