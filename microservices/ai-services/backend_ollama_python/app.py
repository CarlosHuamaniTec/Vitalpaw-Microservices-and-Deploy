import os
import httpx
import json
import logging
import asyncio
import time
import glob
import uuid
from fastapi import FastAPI, HTTPException, UploadFile, File, Form, Depends, status
from fastapi.security import APIKeyHeader
from pydantic import BaseModel
from typing import List, Dict, Any, AsyncGenerator
from fastapi.responses import StreamingResponse
from langchain_ollama import OllamaLLM, OllamaEmbeddings
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_qdrant import QdrantVectorStore
from qdrant_client import QdrantClient
from qdrant_client.models import Distance, VectorParams, Filter, FieldCondition, MatchValue
from slowapi import Limiter, _rate_limit_exceeded_handler
from slowapi.util import get_remote_address
from slowapi.errors import RateLimitExceeded
import redis.asyncio as redis

# Configuración del logger
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler(),
        logging.FileHandler('app.log')
    ]
)
logger = logging.getLogger(__name__)

app = FastAPI(
    title="Ollama Local AI Server with RAG",
    description="API para interactuar con modelos Ollama, gestionar documentación Markdown y realizar RAG.",
    version="1.0.0"
)
limiter = Limiter(key_func=get_remote_address)
app.state.limiter = limiter
app.add_exception_handler(RateLimitExceeded, _rate_limit_exceeded_handler)

# Configuración de variables de entorno
OLLAMA_GPU_HOST = os.getenv("OLLAMA_GPU_HOST", "http://ollama-gpu:11434")
QDRANT_HOST = os.getenv("QDRANT_HOST", "http://qdrant:6333")
REDIS_HOST = os.getenv("REDIS_HOST", "dragonfly")
API_PORT = int(os.getenv("API_PORT", 8000))
LLM_MODEL = os.getenv("LLM_MODEL", "phi4-mini:3.8b")
EMBEDDING_MODEL = os.getenv("EMBEDDING_MODEL", "bge-m3:567m")

# Conexión a Dragonfly
redis_client = redis.Redis(host=REDIS_HOST, port=6379, decode_responses=True)

# Autenticación con API Key
api_key_header = APIKeyHeader(name="X-API-Key", auto_error=False)

async def get_api_key(api_key: str = Depends(api_key_header)):
    if not api_key:
        logger.warning("Intento de acceso sin API Key")
        raise HTTPException(status_code=401, detail="API Key required")
    async with httpx.AsyncClient() as client:
        response = await client.get(
            "http://auth-service:8080/api/auth/validate",
            headers={"X-API-Key": api_key}
        )
        if response.status_code != 200:
            logger.warning("API Key inválida")
            raise HTTPException(status_code=401, detail="Invalid API Key")
    return api_key

# Inicialización de modelos
llm = OllamaLLM(base_url=OLLAMA_GPU_HOST, model=LLM_MODEL)
embeddings = OllamaEmbeddings(base_url=OLLAMA_GPU_HOST, model=EMBEDDING_MODEL)

class GenerateRequest(BaseModel):
    query: str
    stream: bool = False
    collection_name: str = "default_docs"
    document_ids: List[str] = None
    category: str = None
    conversation_id: str = None

class IngestDocResponse(BaseModel):
    status: str
    message: str
    documents_processed: int
    collection_name: str

async def check_session_limit(api_key: str, session_id: str):
    key = f"sessions:{api_key}"
    sessions = await redis_client.smembers(key)
    if len(sessions) >= 2 and session_id not in sessions:
        logger.warning(f"Excedido límite de sesiones para API Key: {api_key}")
        raise HTTPException(status_code=429, detail="Maximum 2 active sessions per API Key")
    await redis_client.sadd(key, session_id)
    await redis_client.expire(key, 3600)  # Sesiones expiran en 1 hora

async def save_message(conversation_id: str, user_id: str, query: str, response: str):
    key = f"conv:{user_id}:{conversation_id}"
    messages = await redis_client.get(key)
    messages = json.loads(messages) if messages else []
    messages.append({"user": query, "bot": response, "timestamp": time.time()})
    if sum(len(json.dumps(msg)) for msg in messages) > 8000:  # Límite aproximado de tokens
        messages = messages[-10:]  # Mantener últimos 10 mensajes
    await redis_client.set(key, json.dumps(messages), ex=86400)  # Expira en 1 día

async def get_conversation(conversation_id: str, user_id: str) -> List[Dict]:
    key = f"conv:{user_id}:{conversation_id}"
    messages = await redis_client.get(key)
    return json.loads(messages) if messages else []

def read_md(file_path: str) -> str:
    logger.info(f"Reading Markdown file: {file_path}")
    with open(file_path, 'r', encoding='utf-8') as f:
        markdown_text = f.read()
    if not markdown_text.strip():
        logger.error(f"Empty Markdown file: {file_path}")
        raise ValueError("The Markdown file is empty.")
    return markdown_text

async def get_vector_store(collection_name: str):
    logger.info(f"Initializing vector store for collection: {collection_name}")
    try:
        client = QdrantClient(url=QDRANT_HOST)
        collections = client.get_collections().collections
        collection_exists = any(c.name == collection_name for c in collections)

        if not collection_exists:
            logger.info(f"Creating collection '{collection_name}'")
            sample_embedding_vector = embeddings.embed_query("test")
            vector_size = len(sample_embedding_vector)

            client.create_collection(
                collection_name=collection_name,
                vectors_config=VectorParams(
                    size=vector_size,
                    distance=Distance.COSINE,
                    hnsw_config={"m": 16, "ef_construct": 100}
                )
            )
            logger.info(f"Collection '{collection_name}' created with size {vector_size}")

        vectorstore = QdrantVectorStore(
            client=client,
            collection_name=collection_name,
            embedding=embeddings
        )
        return vectorstore
    except Exception as e:
        logger.error(f"Error connecting to Qdrant: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Error connecting to Qdrant: {str(e)}")

async def ingest_document(file_path: str, collection_name: str, document_id: str) -> IngestDocResponse:
    logger.info(f"Ingesting document: {file_path} into collection {collection_name}")
    try:
        raw_text = read_md(file_path)
        if len(raw_text) > 100000:
            logger.error(f"File too large: {file_path}")
            raise HTTPException(status_code=400, detail="File exceeds maximum size of 100 KB.")
        text_splitter = RecursiveCharacterTextSplitter(
            chunk_size=500,
            chunk_overlap=100,
            length_function=len,
        )
        texts = text_splitter.split_text(raw_text)
        if len(texts) > 100:
            logger.error(f"Too many fragments generated: {file_path}")
            raise HTTPException(status_code=400, detail="Document generates too many fragments (>100).")
        metadatas = [{"document_id": document_id, "source": os.path.basename(file_path), "chunk_index": i} for i in range(len(texts))]

        vector_store = await get_vector_store(collection_name)
        client = vector_store.client
        
        if collection_name in [c.name for c in client.get_collections().collections]:
            logger.info(f"Deleting previous points for document '{document_id}'")
            delete_filter = Filter(
                must=[
                    FieldCondition(
                        key="document_id",
                        match=MatchValue(value=document_id)
                    )
                ]
            )
            scroll_result = client.scroll(
                collection_name=collection_name,
                scroll_filter=delete_filter,
                limit=100
            )[0]
            if scroll_result:
                point_ids = [point.id for point in scroll_result]
                if point_ids:
                    client.delete(
                        collection_name=collection_name,
                        points_selector={"points": point_ids}
                    )
                    logger.info(f"Deleted {len(point_ids)} previous points")

        start_time = asyncio.get_event_loop().time()
        vector_store.add_texts(texts=texts, metadatas=metadatas)
        end_time = asyncio.get_event_loop().time()
        logger.info(f"Document '{document_id}' processed with {len(texts)} fragments in {end_time - start_time:.2f} seconds")
        return IngestDocResponse(
            status="success",
            message=f"Document '{document_id}' processed and added.",
            documents_processed=len(texts),
            collection_name=collection_name
        )
    except Exception as e:
        logger.error(f"Error during document ingestion: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Error during ingestion: {str(e)}")

@app.on_event("startup")
async def startup_event():
    logger.info("Starting document ingestion")
    collection_name = "default_docs"
    docs_path = "/app/docs/*.md*"
    for file_path in glob.glob(docs_path):
        if file_path.endswith((".md", ".markdown")):
            document_id = os.path.basename(file_path)
            try:
                response = await ingest_document(file_path, collection_name, document_id)
                logger.info(f"Document processed: {document_id}")
            except Exception as e:
                logger.error(f"Error processing document {file_path}: {str(e)}")

@app.get("/health")
async def health_check():
    logger.info("Checking health status...")
    return {"status": "healthy", "message": "Ollama Local AI Server with RAG is running"}

@app.post("/ingest-doc/", response_model=IngestDocResponse, dependencies=[Depends(get_api_key)])
async def ingest_document_endpoint(file: UploadFile = File(...), collection_name: str = Form("default_docs")):
    logger.info(f"Receiving ingestion request for file: {file.filename}")
    if not file.filename.endswith((".md", ".markdown")):
        logger.error(f"Unsupported file type: {file.filename}")
        raise HTTPException(status_code=400, detail="Only Markdown (.md or .markdown) files are supported.")
    
    document_id = file.filename
    file_location = f"/tmp/{file.filename}"
    try:
        with open(file_location, "wb") as f:
            content = await file.read()
            f.write(content)
        response = await ingest_document(file_location, collection_name, document_id)
        return response
    finally:
        if os.path.exists(file_location):
            os.remove(file_location)

@app.post("/query/rag-query/", dependencies=[Depends(get_api_key)])
@limiter.limit("10/minute")
async def rag_query(request: GenerateRequest, api_key: str = Depends(get_api_key)):
    session_id = str(uuid.uuid4())
    await check_session_limit(api_key, session_id)
    user_id = api_key
    conversation_id = request.conversation_id or str(uuid.uuid4())
    try:
        vector_store = await get_vector_store(request.collection_name)
        start_time = time.time()
        docs = vector_store.similarity_search(
            query=request.query,
            k=4,
            filter=Filter(
                must=[
                    FieldCondition(key="document_id", match=MatchValue(value=doc_id))
                    for doc_id in request.document_ids
                ] if request.document_ids else []
            ) if request.document_ids else None
        )
        end_time = time.time()
        logger.info(f"RAG query processed in {end_time - start_time:.2f} seconds, {len(docs)} documents retrieved")
        
        context_text = "\n\n".join([doc.page_content for doc in docs])
        history = await get_conversation(conversation_id, user_id)
        history_text = "\n".join([f"User: {msg['user']}\nBot: {msg['bot']}" for msg in history])
        context_text += f"\n\nConversation History:\n{history_text}"

        prompt_template = f"""You are an AI assistant specialized in AI development and microservices. Use the provided 'Context', which contains technical documentation and code, as the primary source. Provide clear, concise, and detailed technical answers, including code examples if relevant. If the 'Context' does not contain the answer, use your general software development knowledge.

Context:
{context_text}

Question: {request.query}
Answer:"""
        
        source_documents_metadata = [doc.metadata.copy() for doc in docs]

        if request.stream:
            async def generate_stream():
                logger.info("Starting response streaming")
                try:
                    metadata_payload = {"source_documents": source_documents_metadata, "type": "metadata"}
                    yield f"data: {json.dumps(metadata_payload)}\n\n"
                    async for chunk in llm.astream(prompt_template):
                        content_payload = {"content": chunk, "type": "text"}
                        yield f"data: {json.dumps(content_payload)}\n\n"
                    yield "data: {\"type\": \"end\"}\n\n"
                except Exception as e:
                    logger.error(f"Error during streaming: {e}")
                    yield f"data: {json.dumps({'type': 'error', 'message': str(e)})}\n\n"
                
            return StreamingResponse(
                generate_stream(),
                media_type="text/event-stream",
                headers={"Cache-Control": "no-cache", "Connection": "keep-alive"}
            )
        else:
            response_text = await llm.ainvoke(prompt_template)
            logger.info("Non-streaming response generated")
            await save_message(conversation_id, user_id, request.query, response_text)
            return {
                "response": response_text,
                "source_documents": source_documents_metadata,
                "conversation_id": conversation_id
            }
    except Exception as e:
        logger.error(f"Error during RAG query: {e}")
        raise HTTPException(status_code=500, detail=f"Error during RAG query: {e}")
    finally:
        await redis_client.srem(f"sessions:{api_key}", session_id)

@app.get("/conversations/")
async def list_conversations(api_key: str = Depends(get_api_key)):
    user_id = api_key
    keys = await redis_client.keys(f"conv:{user_id}:*")
    return {"conversations": [key.split(":")[-1] for key in keys]}

@app.delete("/conversations/{conversation_id}")
async def delete_conversation(conversation_id: str, api_key: str = Depends(get_api_key)):
    user_id = api_key
    await redis_client.delete(f"conv:{user_id}:{conversation_id}")
    return {"status": "deleted"}