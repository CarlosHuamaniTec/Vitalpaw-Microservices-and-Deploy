import logging
from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse, StreamingResponse
from slowapi.errors import RateLimitExceeded
from fastapi.middleware.cors import CORSMiddleware
from . import config
from .routes import documents, chat  # Import both routers

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[logging.StreamHandler(), logging.FileHandler('app.log')]
)
logger = logging.getLogger(__name__)

app = FastAPI(
    title="Ollama Local AI Server with RAG",
    description="API for interacting with Ollama models, managing Markdown documentation, and performing RAG.",
    version="1.0.0"
)

app.state.limiter = config.limiter  # Use the limiter from config
app.add_exception_handler(RateLimitExceeded, config._rate_limit_exceeded_handler)

# Middleware to log validation errors
@app.middleware("http")
async def log_validation_errors(request: Request, call_next):
    body = None
    try:
        # Obtener el cuerpo de la solicitud si es POST o PUT
        body = await request.json() if request.method in ["POST", "PUT"] else None
    except Exception as e:
        body = f"Unable to parse body: {str(e)}"
    try:
        response = await call_next(request)  # Pasar el objeto Request correctamente
        if response.status_code == 422:
            try:
                response_body = await response.body() if hasattr(response, 'body') else "Streaming response (details unavailable)"
                logger.error(f"Validation error for request {request.url}: Body={body}, Response={response_body.decode('utf-8') if isinstance(response_body, bytes) else response_body}")
            except Exception as e:
                logger.error(f"Error logging validation details for {request.url}: {str(e)}")
        return response
    except Exception as e:
        logger.error(f"Unexpected error in middleware for {request.url}: {str(e)}")
        raise

# CORS configuration (actualizado para incluir ai-api.local)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://chatbot.local", "http://ai-api.local"],  # Agregado ai-api.local para Swagger UI
    allow_credentials=config.CORS_ALLOW_CREDENTIALS,
    allow_methods=config.CORS_METHODS,
    allow_headers=config.CORS_HEADERS,
)

@app.on_event("startup")
async def startup_event():
    logger.info("Initializing application clients...")
    await config.initialize_all_clients()
    logger.info("Application clients initialized successfully.")

@app.on_event("shutdown")
async def shutdown_event():
    logger.info("Shutting down application clients...")
    await config.cleanup_all_clients()
    logger.info("Application clients cleaned up.")

app.include_router(documents.router)
app.include_router(chat.router)  # Include the chat router dynamically

@app.get("/")
async def read_root():
    return {"message": "Welcome to the Ollama Local AI Server API! Check /docs for API documentation."}