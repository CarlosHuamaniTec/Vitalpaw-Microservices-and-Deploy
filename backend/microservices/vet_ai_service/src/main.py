from fastapi import FastAPI, HTTPException
from .model import VetAI
import json

app = FastAPI()
vet_ai = VetAI(model_path="models/finetuned_tinyllama_vet")

@app.get("/vet/query")
async def query_vet(question: str):
    if not question:
        raise HTTPException(status_code=400, detail="Question cannot be empty")
    response = vet_ai.generate_response(question)
    response += " Always consult a veterinarian for a professional diagnosis."
    return {"response": response}

@app.get("/health")
async def health_check():
    return {"status": "healthy"}