from transformers import AutoModelForCausalLM, AutoTokenizer
import torch

class VetAI:
    def __init__(self, model_path: str):
        self.tokenizer = AutoTokenizer.from_pretrained(model_path)
        self.model = AutoModelForCausalLM.from_pretrained(model_path)
        self.model.to("cuda" if torch.cuda.is_available() else "cpu")

    def generate_response(self, question: str) -> str:
        inputs = self.tokenizer(question, return_tensors="pt").to(self.model.device)
        outputs = self.model.generate(**inputs, max_length=150, num_return_sequences=1)
        return self.tokenizer.decode(outputs[0], skip_special_tokens=True)