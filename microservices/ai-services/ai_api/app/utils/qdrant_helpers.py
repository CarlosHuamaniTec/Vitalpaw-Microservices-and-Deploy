import logging
from fastapi import HTTPException, status
from qdrant_client import QdrantClient
from langchain_qdrant import QdrantVectorStore
from langchain_ollama import OllamaEmbeddings
from qdrant_client.models import Distance, VectorParams

logger = logging.getLogger(__name__)

async def get_or_create_vector_store(collection_name: str, qdrant_client: QdrantClient, embeddings_model: OllamaEmbeddings) -> QdrantVectorStore:
    logger.info(f"Attempting to get or create vector store for collection: '{collection_name}'")
    try:
        collections_info = qdrant_client.get_collections().collections
        collection_exists = any(c.name == collection_name for c in collections_info)
        if not collection_exists:
            logger.info(f"Collection '{collection_name}' does not exist. Creating new collection.")
            sample_embedding_vector = embeddings_model.embed_query("test embedding size")
            vector_size = len(sample_embedding_vector)
            qdrant_client.create_collection(
                collection_name=collection_name,
                vectors_config=VectorParams(
                    size=vector_size,
                    distance=Distance.COSINE,
                    hnsw_config={"m": 16, "ef_construct": 100}
                )
            )
            logger.info(f"Collection '{collection_name}' created successfully with vector size {vector_size}.")
        vectorstore = QdrantVectorStore(
            client=qdrant_client,
            collection_name=collection_name,
            embedding=embeddings_model
        )
        logger.info(f"QdrantVectorStore instance ready for collection '{collection_name}'.")
        return vectorstore
    except Exception as e:
        logger.error(f"Error connecting to Qdrant or managing collection '{collection_name}': {str(e)}", exc_info=True)
        raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail=f"Error with Qdrant service during collection management: {str(e)}")