import json
import time
import uuid
from fastapi import APIRouter, Depends, HTTPException, status, Request
from fastapi.responses import StreamingResponse
from typing import List, Dict, Any, AsyncGenerator, Optional
import logging
from .. import config
from .. import security
from ..types import ChatRequest, ChatResponse
from langchain_qdrant import QdrantVectorStore
from qdrant_client.models import Filter, FieldCondition, MatchValue

logger = logging.getLogger(__name__)

router = APIRouter(
    prefix="/chat",
    tags=["AI Interactions"],
    dependencies=[Depends(security.verify_api_key)],
    responses={
        status.HTTP_401_UNAUTHORIZED: {"description": "Unauthorized"},
        status.HTTP_429_TOO_MANY_REQUESTS: {"description": "Too Many Requests"}
    }
)

RAG_PROMPT_ONLY_DOCUMENTATION = """You are an AI assistant specialized in providing answers strictly based on the provided 'Context'. The context contains technical documentation and code.
If the answer is not found within the provided context, state clearly that you cannot answer based on the current documentation. Do not invent information.
Provide clear, concise, and detailed technical answers, including code examples if relevant.

Context:
{context_placeholder}

Question: {question_placeholder}
Answer:"""

RAG_PROMPT_NO_DOCUMENTATION = """You are an AI assistant specialized in software development, QA testing, electronics, Arduino, and related hardware/software fields. Use the provided 'Context' (technical documentation) as your primary source if it's relevant. If the context does not contain the answer, or if the question is general, use your extensive knowledge in software and hardware development.
Do not focus on topics unrelated to software or hardware (e.g., history, arts, biology, general science unrelated to engineering). Provide clear, concise, and detailed technical answers, including code examples if relevant.

Context:
{context_placeholder}

Question: {question_placeholder}
Answer:"""

def _get_prompt_template_by_mode(mode: str) -> str:
    if mode == "only_documentation":
        return RAG_PROMPT_ONLY_DOCUMENTATION
    elif mode == "no_documentation":
        return RAG_PROMPT_NO_DOCUMENTATION
    else:
        logger.error(f"Invalid mode requested: '{mode}'.")
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=f"Invalid mode '{mode}'. Accepted modes are 'only_documentation' and 'no_documentation'.")

async def _check_session_limit(request: Request, api_key: str, session_id: str):
    session_limit = 2
    session_expire_time = 3600
    key = f"sessions:{api_key}"
    sessions = await config.redis_client_instance.smembers(key)
    if len(sessions) >= session_limit and session_id not in sessions:
        logger.warning(f"Session limit ({session_limit}) exceeded for API Key: {api_key}. Current sessions: {sessions}")
        raise HTTPException(status_code=status.HTTP_429_TOO_MANY_REQUESTS, detail=f"Maximum {session_limit} active sessions per API Key.")
    await config.redis_client_instance.sadd(key, session_id)
    await config.redis_client_instance.expire(key, session_expire_time)
    logger.info(f"Session {session_id} registered for API Key: {api_key}. Total active sessions: {len(sessions) + 1}")

async def _remove_session(api_key: str, session_id: str):
    key = f"sessions:{api_key}"
    await config.redis_client_instance.srem(key, session_id)
    logger.info(f"Session {session_id} removed for API Key: {api_key}.")

async def _save_message_to_history(conversation_id: str, user_id: str, query: str, response: str):
    conversation_expire_time = 86400
    conversation_max_size_bytes = 8000
    conversation_max_messages = 10
    key = f"conv:{user_id}:{conversation_id}"
    messages_json = await config.redis_client_instance.get(key)
    messages = json.loads(messages_json) if messages_json else []
    messages.append({"user": query, "bot": response, "timestamp": time.time()})
    current_size = sum(len(json.dumps(msg)) for msg in messages)
    while current_size > conversation_max_size_bytes and len(messages) > 1:
        messages.pop(0)
        current_size = sum(len(json.dumps(msg)) for msg in messages)
    if len(messages) > conversation_max_messages:
        messages = messages[-conversation_max_messages:]
        logger.warning(f"Conversation {conversation_id} for {user_id} truncated due to message count limit ({conversation_max_messages}).")
    await config.redis_client_instance.set(key, json.dumps(messages), ex=conversation_expire_time)
    logger.info(f"Message saved for conversation {conversation_id} by user {user_id}. Current messages count: {len(messages)}.")

async def _get_conversation_history(conversation_id: str, user_id: str) -> List[Dict]:
    key = f"conv:{user_id}:{conversation_id}"
    messages_json = await config.redis_client_instance.get(key)
    return json.loads(messages_json) if messages_json else []

async def _perform_rag_document_search(query: str, collection_name: str, document_ids: Optional[List[str]]) -> List[Any]:
    logger.info(f"Performing RAG query in collection '{collection_name}' for query: '{query[:50]}...'")
    try:
        vector_store = QdrantVectorStore(
            client=config.qdrant_client_instance,
            collection_name=collection_name,
            embedding=config.ollama_embeddings_instance
        )
        rag_filter = None
        if document_ids:
            rag_filter = Filter(
                must=[
                    FieldCondition(key="document_id", match=MatchValue(value=doc_id))
                    for doc_id in document_ids
                ]
            )
        docs = vector_store.similarity_search(
            query=query,
            k=4,
            filter=rag_filter
        )
        logger.info(f"Retrieved {len(docs)} documents for RAG query from collection '{collection_name}'.")
        return docs
    except Exception as e:
        logger.error(f"Error performing RAG document search: {str(e)}", exc_info=True)
        raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail=f"RAG document search service error: {str(e)}")

async def _generate_llm_response(prompt: str, stream: bool = False) -> Any:
    logger.info(f"Generating LLM response (stream={stream}) for prompt starting with: '{prompt[:100]}...'")
    try:
        if stream:
            async def stream_generator() -> AsyncGenerator[str, None]:
                async for chunk in config.ollama_llm_instance.astream(prompt):
                    yield chunk
            return stream_generator()
        else:
            response_text = await config.ollama_llm_instance.ainvoke(prompt)
            return response_text
    except Exception as e:
        logger.error(f"Error generating LLM response from Ollama: {str(e)}", exc_info=True)
        raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail=f"Ollama service error: {str(e)}")

def apply_limiter():
    return config.limiter.limit("10/minute")

@router.post("/rag-query", response_model=ChatResponse)
@apply_limiter()
async def rag_query_endpoint(request: Request, data: ChatRequest, api_key: str = Depends(security.verify_api_key)):
    session_id = str(uuid.uuid4())
    user_id = api_key
    conversation_id = data.conversation_id or str(uuid.uuid4())
    try:
        await _check_session_limit(request, user_id, session_id)
        start_rag_time = time.time()
        docs = await _perform_rag_document_search(
            query=data.query, 
            collection_name=data.collection_name, 
            document_ids=data.document_ids
        )
        end_rag_time = time.time()
        logger.info(f"RAG document search completed in {end_rag_time - start_rag_time:.2f} seconds.")
        context_text = "\n\n".join([doc.page_content for doc in docs])
        source_documents_metadata = [doc.metadata.copy() for doc in docs]
        history = await _get_conversation_history(conversation_id, user_id)
        history_text = "\n".join([f"User: {msg['user']}\nBot: {msg['bot']}" for msg in history])
        prompt_template_str = _get_prompt_template_by_mode(data.mode)
        combined_context_and_history = ""
        if context_text.strip():
            combined_context_and_history += f"Context:\n{context_text}"
        if history_text.strip():
            if combined_context_and_history:
                combined_context_and_history += "\n\n"
            combined_context_and_history += f"Conversation History:\n{history_text}"
        final_prompt = prompt_template_str.format(
            context_placeholder=combined_context_and_history,
            question_placeholder=data.query
        )
        if data.stream:
            async def generate_stream():
                full_response_content = ""
                try:
                    metadata_payload = {"source_documents": source_documents_metadata, "type": "metadata"}
                    yield f"data: {json.dumps(metadata_payload)}\n\n"
                    async for chunk in _generate_llm_response(prompt=final_prompt, stream=True):
                        full_response_content += chunk
                        content_payload = {"content": chunk, "type": "text"}
                        yield f"data: {json.dumps(content_payload)}\n\n"
                    await _save_message_to_history(conversation_id, user_id, data.query, full_response_content)
                    yield "data: {\"type\": \"end\"}\n\n"
                    logger.info(f"Streaming response completed for conversation {conversation_id}.")
                except HTTPException as e:
                    logger.error(f"HTTP error during RAG streaming for user {user_id}: {e.detail}", exc_info=True)
                    yield f"data: {json.dumps({'type': 'error', 'message': e.detail})}\n\n"
                except Exception as e:
                    logger.error(f"Unexpected error during RAG streaming for user {user_id}: {e}", exc_info=True)
                    yield f"data: {json.dumps({'type': 'error', 'message': str(e)})}\n\n"
            return StreamingResponse(
                generate_stream(),
                media_type="text/event-stream",
                headers={"Cache-Control": "no-cache", "Connection": "keep-alive"}
            )
        else:
            response_text = await _generate_llm_response(prompt=final_prompt, stream=False)
            await _save_message_to_history(conversation_id, user_id, data.query, response_text)
            logger.info(f"Non-streaming response generated for conversation {conversation_id}.")
            return ChatResponse(
                response=response_text,
                source_documents=source_documents_metadata,
                conversation_id=conversation_id
            )
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"An unexpected error occurred during RAG query for user {user_id}: {e}", exc_info=True)
        raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail=f"An unexpected error occurred during RAG query: {e}")
    finally:
        await _remove_session(user_id, session_id)

@router.get("/conversations")
async def list_conversations_endpoint(api_key: str = Depends(security.verify_api_key)):
    user_id = api_key
    try:
        keys = await config.redis_client_instance.keys(f"conv:{user_id}:*")
        conversations_list = []
        for key in keys:
            conv_id = key.split(":")[-1]
            data = await config.redis_client_instance.get(key)
            if data:
                conv_data = json.loads(data)
                conv_name = conv_data[0].get("user", f"Conversación {conv_id[:8]}") if conv_data else f"Conversación {conv_id[:8]}"
                conversations_list.append({"conversation_id": conv_id, "name": conv_name})
        logger.info(f"Listed {len(conversations_list)} conversations for user {user_id}.")
        return {"conversations": conversations_list}
    except Exception as e:
        logger.error(f"Error listing conversations for user {user_id}: {e}", exc_info=True)
        raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail=f"Failed to list conversations: {e}")

@router.get("/conversations/{conversation_id}")
async def get_conversation_endpoint(conversation_id: str, api_key: str = Depends(security.verify_api_key)):
    user_id = api_key
    try:
        key = f"conv:{user_id}:{conversation_id}"
        messages_json = await config.redis_client_instance.get(key)
        if not messages_json:
            logger.warning(f"Conversation {conversation_id} not found for user {user_id}.")
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Conversation not found.")
        messages = json.loads(messages_json)
        logger.info(f"Retrieved {len(messages)} messages for conversation {conversation_id}.")
        return {"conversation_id": conversation_id, "messages": messages}
    except Exception as e:
        logger.error(f"Error retrieving conversation {conversation_id} for user {user_id}: {e}", exc_info=True)
        raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail=f"Failed to retrieve conversation: {e}")

@router.delete("/conversations/{conversation_id}")
async def delete_conversation_endpoint(conversation_id: str, api_key: str = Depends(security.verify_api_key)):
    user_id = api_key
    try:
        key = f"conv:{user_id}:{conversation_id}"
        if not await config.redis_client_instance.exists(key):
            logger.warning(f"Attempted to delete non-existent conversation {conversation_id} for user {user_id}.")
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Conversation not found.")
        await config.redis_client_instance.delete(key)
        logger.info(f"Conversation '{conversation_id}' deleted for user {user_id}.")
        return {"status": "success", "message": f"Conversation '{conversation_id}' deleted."}
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error deleting conversation {conversation_id} for user {user_id}: {e}", exc_info=True)
        raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail=f"Failed to delete conversation: {e}")