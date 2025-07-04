import os
import logging
import redis.asyncio as redis
from qdrant_client import QdrantClient
from qdrant_client.models import Distance, VectorParams
from langchain_ollama import OllamaLLM, OllamaEmbeddings
import httpx
from slowapi import Limiter, _rate_limit_exceeded_handler
from slowapi.util import get_remote_address

logger = logging.getLogger(__name__)

# --- Variables de Entorno y Configuración de Servicios ---
OLLAMA_GPU_HOST = os.getenv("OLLAMA_GPU_HOST", "http://ollama-gpu:11434")
QDRANT_HOST = os.getenv("QDRANT_HOST", "http://qdrant:6333")
REDIS_HOST = os.getenv("REDIS_HOST", "dragonfly")
AUTH_SERVICE_URL = os.getenv("AUTH_SERVICE_URL", "http://auth-service:8080")
LLM_MODEL = os.getenv("LLM_MODEL", "phi4-mini:3.8b")
EMBEDDING_MODEL = os.getenv("EMBEDDING_MODEL", "bge-m3:567m")
API_PORT = int(os.getenv("API_PORT", 8000))

# --- Configuración CORS ---
CORS_ORIGINS = ["http://chatbot.local"]
CORS_METHODS = ["*"]
CORS_HEADERS = ["*"]
CORS_ALLOW_CREDENTIALS = True

# --- Instancias de Clientes Globales ---
redis_client_instance: redis.Redis = None
qdrant_client_instance: QdrantClient = None
ollama_llm_instance: OllamaLLM = None
ollama_embeddings_instance: OllamaEmbeddings = None
http_client_instance: httpx.AsyncClient = None

# --- Configuración del Limiter ---
limiter = Limiter(key_func=get_remote_address)

async def initialize_all_clients():
    global redis_client_instance, qdrant_client_instance, ollama_llm_instance, ollama_embeddings_instance, http_client_instance
    try:
        http_client_instance = httpx.AsyncClient()
        logger.info("HTTP client initialized.")
        redis_client_instance = redis.Redis(host=REDIS_HOST, port=6379, decode_responses=True)
        await redis_client_instance.ping()
        logger.info("Dragonfly (Redis) client initialized and connected.")
        qdrant_client_instance = QdrantClient(url=QDRANT_HOST)
        logger.info("Qdrant client initialized and connected.")
        ollama_llm_instance = OllamaLLM(base_url=OLLAMA_GPU_HOST, model=LLM_MODEL)
        ollama_embeddings_instance = OllamaEmbeddings(base_url=OLLAMA_GPU_HOST, model=EMBEDDING_MODEL)
        ollama_test_response = await http_client_instance.get(f"{OLLAMA_GPU_HOST}/api/tags", timeout=5)
        ollama_test_response.raise_for_status()
        logger.info(f"Ollama LLM ({LLM_MODEL}) and Embeddings ({EMBEDDING_MODEL}) initialized and reachable.")
    except Exception as e:
        logger.critical(f"FATAL: Failed to initialize clients: {e}", exc_info=True)
        raise RuntimeError(f"Application failed to start due to client initialization error: {e}")

async def cleanup_all_clients():
    if redis_client_instance:
        await redis_client_instance.close()
        logger.info("Dragonfly (Redis) client closed.")
    if http_client_instance:
        await http_client_instance.aclose()
        logger.info("HTTP client closed.")