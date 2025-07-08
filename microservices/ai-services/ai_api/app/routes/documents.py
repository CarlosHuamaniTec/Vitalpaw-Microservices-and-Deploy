import os
import uuid
from fastapi import APIRouter, UploadFile, File, Form, HTTPException, status, Depends
from langchain_text_splitters import RecursiveCharacterTextSplitter
from qdrant_client.models import Filter, FieldCondition, MatchValue, FilterSelector
from langchain_qdrant import QdrantVectorStore
from typing import Dict, Any
import logging
from .. import config
from .. import security
from ..types import IngestDocumentResponse
from ..utils.file_operations import read_markdown_file
from ..utils.qdrant_helpers import get_or_create_vector_store

logger = logging.getLogger(__name__)

router = APIRouter(
    prefix="/documents",
    tags=["Document Management"],
    dependencies=[Depends(security.verify_api_key)],
    responses={status.HTTP_401_UNAUTHORIZED: {"description": "Unauthorized"}}
)

@router.post("/ingest", response_model=IngestDocumentResponse)
async def ingest_document_endpoint(
    file: UploadFile = File(...),
    collection_name: str = Form("default_docs"),
    api_key: str = Depends(security.verify_api_key),  # Añadido explícitamente para depuración
):
    logger.info(f"Received ingestion request for file: {file.filename} into collection: {collection_name}")
    if not file.filename.endswith((".md", ".markdown")):
        logger.error(f"Unsupported file type for {file.filename}.")
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Only Markdown (.md or .markdown) files are supported.")
    document_id = file.filename
    temp_dir = "/tmp"
    os.makedirs(temp_dir, exist_ok=True)
    temp_file_location = os.path.join(temp_dir, f"{file.filename}_{uuid.uuid4().hex}")
    try:
        with open(temp_file_location, "wb") as f:
            content = await file.read()
            f.write(content)
        raw_text = read_markdown_file(temp_file_location)
        if len(raw_text) > 100000:
            raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="File exceeds maximum size of 100 KB.")
        text_splitter = RecursiveCharacterTextSplitter(
            chunk_size=500,
            chunk_overlap=100,
            length_function=len,
        )
        texts = text_splitter.split_text(raw_text)
        if len(texts) > 100:
            raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Document generates too many fragments (>100).")
        metadatas = [{"document_id": document_id, "source": os.path.basename(file.filename), "chunk_index": i} for i in range(len(texts))]
        vector_store = await get_or_create_vector_store(
            collection_name, 
            config.qdrant_client_instance, 
            config.ollama_embeddings_instance
        )
        logger.info(f"Checking for and deleting previous points for document '{document_id}'")
        delete_filter = Filter(
            must=[
                FieldCondition(
                    key="document_id",
                    match=MatchValue(value=document_id)
                )
            ]
        )
        config.qdrant_client_instance.delete(
            collection_name=collection_name,
            points_selector=FilterSelector(filter=delete_filter)  # Corrección aquí
        )
        logger.info(f"Previous points for '{document_id}' deleted if existed.")
        vector_store.add_texts(texts=texts, metadatas=metadatas)
        logger.info(f"Document '{document_id}' processed with {len(texts)} fragments.")
        return IngestDocumentResponse(
            status="success",
            message=f"Document '{document_id}' processed and added.",
            documents_processed=len(texts),
            collection_name=collection_name
        )
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"An unexpected error occurred during ingestion of {file.filename}: {e}", exc_info=True)
        raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail=f"An unexpected error occurred: {e}")
    finally:
        if os.path.exists(temp_file_location):
            os.remove(temp_file_location)
            logger.info(f"Cleaned up temporary file: {temp_file_location}")