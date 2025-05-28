from fastapi import FastAPI, HTTPException
from .model import SupAI
import json

app = FastAPI()
support_ai = SupAI(model_path="models/finetuned_tinyllama_sup")

@app.get("/support/query")
async def query_support(question: str):
    if not question:
        raise HTTPException(status_code=400, detail="Question cannot be empty")
    response = support_ai.generate_response(question)
    return {"response": response}

@app.get("/health")
async def health_check():
    return {"status": "healthy"}