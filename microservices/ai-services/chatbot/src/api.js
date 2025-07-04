const AI_API_URL = 'http://ai-api.local';

export const fetchConversations = async (apiKey) => {
  try {
    const response = await fetch(`${AI_API_URL}/chat/conversations`, {
      method: 'GET',
      headers: {
        'X-API-Key': apiKey,
        'Content-Type': 'application/json',
      },
    });
    if (!response.ok) {
      const errorData = await response.json().catch(() => ({ detail: response.statusText }));
      throw new Error(`Error ${response.status}: ${errorData.detail || 'Unknown error'}`);
    }
    const data = await response.json();
    return data.conversations || [];
  } catch (error) {
    console.error('Error fetching conversations:', error);
    throw new Error(`Error al cargar el historial de conversaciones: ${error.message}`);
  }
};

export const fetchConversation = async (apiKey, conversationId) => {
  try {
    const response = await fetch(`${AI_API_URL}/chat/conversations/${conversationId}`, {
      method: 'GET',
      headers: {
        'X-API-Key': apiKey,
        'Content-Type': 'application/json',
      },
    });
    if (!response.ok) {
      const errorData = await response.json().catch(() => ({ detail: response.statusText }));
      throw new Error(`Error ${response.status}: ${errorData.detail || 'Unknown error'}`);
    }
    return await response.json();
  } catch (error) {
    console.error('Error fetching conversation:', error);
    throw new Error(`Error al cargar la conversación: ${error.message}`);
  }
};

export const deleteConversation = async (apiKey, conversationId) => {
  try {
    const response = await fetch(`${AI_API_URL}/chat/conversations/${conversationId}`, {
      method: 'DELETE',
      headers: {
        'X-API-Key': apiKey,
        'Content-Type': 'application/json',
      },
    });
    if (!response.ok) {
      const errorData = await response.json().catch(() => ({ detail: response.statusText }));
      throw new Error(`Error ${response.status}: ${errorData.detail || 'Unknown error'}`);
    }
    return await response.json();
  } catch (error) {
    console.error(`Error deleting conversation ${conversationId}:`, error);
    throw new Error(`Error al eliminar la conversación: ${error.message}`);
  }
};

export const streamChatResponse = (apiKey, query, mode, conversationId, onNewMessage, onComplete, onError) => {
  console.log('Mode sent to streamChatResponse:', mode);
  const url = `${AI_API_URL}/chat/rag-query`;
  const body = {
    query,
    mode,
    stream: true,
    collection_name: "default_docs",
  };
  if (conversationId) {
    body.conversation_id = conversationId;
  }

  fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-API-Key': apiKey,
    },
    body: JSON.stringify(body),
  })
  .then(response => {
    if (!response.ok) {
      return response.json().then(errorData => {
        throw new Error(`HTTP error! status: ${response.status}, message: ${errorData.detail || 'Unknown error'}`);
      }).catch(() => {
        throw new Error(`HTTP error! status: ${response.status}, message: ${response.statusText}`);
      });
    }
    const reader = response.body.getReader();
    const decoder = new TextDecoder('utf-8');
    let buffer = '';

    const readStream = () => {
      reader.read().then(({ done, value }) => {
        if (done) {
          onComplete();
          return;
        }

        buffer += decoder.decode(value, { stream: true });
        const parts = buffer.split('\n\n');
        buffer = parts.pop();

        parts.forEach(part => {
          if (part.startsWith('data: ')) {
            try {
              const jsonStr = part.substring(6);
              const data = JSON.parse(jsonStr);
              onNewMessage(data);
            } catch (e) {
              console.error('Error parsing SSE chunk:', e, 'Chunk:', part);
              onError({ type: 'error', message: 'Error procesando la respuesta del servidor.' });
            }
          }
        });

        readStream();
      }).catch(error => {
        console.error('Error reading stream:', error);
        onError({ type: 'error', message: `Error de conexión con el servicio de IA: ${error.message}` });
      });
    };

    readStream();
  })
  .catch(error => {
    console.error('Error initiating chat stream:', error);
    onError({ type: 'error', message: `Error al iniciar la conversación: ${error.message}` });
  });
};