import os
import httpx
from fastapi import FastAPI, HTTPException, UploadFile, File, Form
from pydantic import BaseModel
from typing import List, Dict, Any, AsyncGenerator
from fastapi.responses import StreamingResponse
import json # ¡Importamos la librería json!

from langchain_community.llms import Ollama
from langchain_community.embeddings import OllamaEmbeddings
from langchain_text_splitters import RecursiveCharacterTextSplitter

from langchain_community.vectorstores import Qdrant
from qdrant_client import QdrantClient
from qdrant_client.models import Distance, VectorParams, Filter, FieldCondition, MatchValue


app = FastAPI(
    title="Ollama Local AI Server with RAG",
    description="API para interactuar con modelos Ollama (Phi4-Mini), gestionar documentación Markdown y realizar RAG.",
    version="1.0.0"
)

OLLAMA_GPU_HOST = os.getenv("OLLAMA_GPU_HOST", "http://ollama-gpu:11434")
QDRANT_HOST = os.getenv("QDRANT_HOST", "http://qdrant:6333")
API_PORT = int(os.getenv("API_PORT", 8000))
LLM_MODEL = os.getenv("LLM_MODEL", "phi4-mini:3.8b")
EMBEDDING_MODEL = os.getenv("EMBEDDING_MODEL", "bge-m3:567m") 

class GenerateRequest(BaseModel):
    query: str
    stream: bool = False
    collection_name: str = "default_docs"

class IngestDocResponse(BaseModel):
    status: str
    message: str
    documents_processed: int
    collection_name: str

llm = Ollama(base_url=OLLAMA_GPU_HOST, model=LLM_MODEL)
embeddings = OllamaEmbeddings(base_url=OLLAMA_GPU_HOST, model=EMBEDDING_MODEL)

def read_md(file_path: str) -> str:
    """Lee el contenido de un archivo Markdown."""
    with open(file_path, 'r', encoding='utf-8') as f:
        markdown_text = f.read()
    return markdown_text

async def get_vector_store(collection_name: str):
    """
    Inicializa y devuelve el vector store de Qdrant.
    Crea la colección si no existe.
    """
    try:
        client = QdrantClient(url=QDRANT_HOST)
        
        collections = client.get_collections().collections
        collection_exists = any(c.name == collection_name for c in collections)

        if not collection_exists:
            print(f"La colección '{collection_name}' no existe. Creándola...")
            sample_embedding_vector = embeddings.embed_query("test")
            vector_size = len(sample_embedding_vector)

            client.recreate_collection(
                collection_name=collection_name,
                vectors_config=VectorParams(size=vector_size, distance=Distance.COSINE)
            )
            print(f"Colección '{collection_name}' creada en Qdrant con tamaño {vector_size}.")

        vectorstore = Qdrant(
            client=client,
            collection_name=collection_name,
            embeddings=embeddings 
        )
        return vectorstore
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error conectando a Qdrant o gestionando la colección: {e}")

@app.get("/")
async def read_root():
    """Endpoint de salud básico del servicio."""
    return {"message": "Servidor Ollama Local AI con RAG está corriendo!"}

@app.post("/ingest-doc/", response_model=IngestDocResponse)
async def ingest_document(file: UploadFile = File(...), collection_name: str = Form("default_docs")):
    """
    Ingiere un documento Markdown en la base de datos vectorial Qdrant.
    Si un documento con el mismo nombre de archivo ya existe, sus fragmentos
    anteriores serán eliminados y reemplazados por los nuevos.
    """
    try:
        if not file.filename.endswith(".md"):
            raise HTTPException(status_code=400, detail="Solo se soportan archivos Markdown (.md) para la ingestión.")

        document_id = file.filename

        file_location = f"/tmp/{file.filename}"
        with open(file_location, "wb") as f:
            f.write(file.file.read())

        raw_text = read_md(file_location)
        os.remove(file_location)

        text_splitter = RecursiveCharacterTextSplitter(
            chunk_size=1000, 
            chunk_overlap=200,
            length_function=len,
        )
        texts = text_splitter.split_text(raw_text)

        metadatas = [{"document_id": document_id, "source": file.filename, "chunk_index": i} for i, _ in enumerate(texts)]

        vector_store = await get_vector_store(collection_name)
        qdrant_client_instance = vector_store.client
        
        try:
            if collection_name in [c.name for c in qdrant_client_instance.get_collections().collections]:
                print(f"Buscando puntos existentes para el documento '{document_id}' en la colección '{collection_name}'.")
                
                delete_filter = Filter(
                    must=[
                        FieldCondition(
                            key="document_id",
                            match=MatchValue(value=document_id)
                        )
                    ]
                )
                
                scroll_result, _ = qdrant_client_instance.scroll(
                    collection_name=collection_name,
                    scroll_filter=delete_filter,
                    limit=0,
                    with_payload=False,
                    with_vectors=False
                )
                points_to_delete_count = len(scroll_result)

                if points_to_delete_count > 0:
                    print(f"Eliminando {points_to_delete_count} puntos anteriores del documento '{document_id}'.")
                    qdrant_client_instance.delete(
                        collection_name=collection_name,
                        points_selector={"filter": delete_filter}
                    )
                    print(f"Puntos del documento '{document_id}' eliminados exitosamente.")
                else:
                    print(f"No se encontraron puntos anteriores para el documento '{document_id}'. Procediendo con la ingestión.")
            else:
                print(f"La colección '{collection_name}' no existe, se creará al añadir los nuevos puntos.")
            
        except Exception as e:
            print(f"Advertencia: Error al intentar eliminar puntos anteriores para el documento '{document_id}': {e}")

        vector_store.add_texts(texts=texts, metadatas=metadatas)

        return IngestDocResponse(
            status="success",
            message=f"Documento '{file.filename}' procesado y sus fragmentos han sido actualizados/añadidos.",
            documents_processed=len(texts),
            collection_name=collection_name
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error durante la ingestión del documento: {e}")

@app.post("/rag-query/")
async def rag_query(request: GenerateRequest):
    """
    Realiza una consulta RAG utilizando los documentos ingestados o conocimientos generales de software.
    Soporta respuestas en streaming.
    """
    try:
        vector_store = await get_vector_store(request.collection_name)
        
        docs = vector_store.similarity_search(query=request.query, k=8) 
        
        context_text = "\n\n".join([doc.page_content for doc in docs])
        
        prompt_template = f"""Eres un asistente de IA especializado en el ámbito del desarrollo de software. Tu objetivo es proporcionar respuestas útiles y precisas.

Utiliza la información del siguiente 'Contexto' como tu fuente principal de conocimiento para preguntas específicas relacionadas con la documentación proporcionada. Si el 'Contexto' es limitado o no contiene la respuesta directa, utiliza tus conocimientos generales sobre desarrollo de software, arquitectura de sistemas, metodologías, lenguajes de programación, herramientas o prácticas de la industria para formular una respuesta.

Mantén tus respuestas claras, concisas y siempre relevantes para el dominio del software. Evita desviarte a temas no relacionados con la programación. Si una pregunta está completamente fuera del ámbito del software, por favor, indícalo.

Contexto:
{context_text}

Pregunta: {request.query}
Respuesta:"""

        source_documents_metadata = []
        for doc in docs:
            doc_metadata = doc.metadata.copy() 
            source_documents_metadata.append(doc_metadata)

        if request.stream:
            async def generate_stream():
                # Prepara el payload de metadatos como un diccionario Python
                metadata_payload = {"source_documents": source_documents_metadata, "type": "metadata"}
                # Convierte el diccionario a una cadena JSON y la incluye en la f-string
                yield f"data: {json.dumps(metadata_payload)}\n\n"
                
                for chunk in llm.stream(prompt_template):
                    # Prepara el payload de contenido como un diccionario Python
                    content_payload = {"content": chunk.content, "type": "text"}
                    # Convierte el diccionario a una cadena JSON y la incluye en la f-string
                    yield f"data: {json.dumps(content_payload)}\n\n"
                
                yield "data: {\"type\": \"end\"}\n\n"

            return StreamingResponse(generate_stream(), media_type="text/event-stream")
        else:
            response_text = llm.invoke(prompt_template)
            return {"response": response_text, "source_documents": source_documents_metadata}

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error durante la consulta RAG: {e}")