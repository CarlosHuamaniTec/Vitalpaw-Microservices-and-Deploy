from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any

class IngestDocumentResponse(BaseModel):
    status: str = Field(...)
    message: str = Field(...)
    documents_processed: int = Field(...)
    collection_name: str = Field(...)

class ChatRequest(BaseModel):
    query: str = Field(...)
    mode: str = Field("only_documentation")
    stream: bool = Field(False)
    collection_name: str = Field("default_docs")
    document_ids: Optional[List[str]] = Field(None)
    conversation_id: Optional[str] = Field(None)

class ChatResponse(BaseModel):
    response: str = Field(...)
    source_documents: List[dict] = Field(...)
    conversation_id: str = Field(...)