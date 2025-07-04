import React, { useState, useEffect, useRef } from 'react';
import ChatInput from './ChatInput';
import Message from './Message';
import ModeSelector from './ModeSelector';
import History from './History';
import { streamChatResponse, fetchConversations, deleteConversation, fetchConversation } from '../api';

function Chat({ apiKey, onLogout }) {
  const [messages, setMessages] = useState([]);
  const [isLoadingResponse, setIsLoadingResponse] = useState(false);
  const [chatError, setChatError] = useState('');
  const [currentConversationId, setCurrentConversationId] = useState(null);
  const [mode, setMode] = useState('only_documentation');
  const [conversations, setConversations] = useState([]);
  const [showHistory, setShowHistory] = useState(false);

  const messagesEndRef = useRef(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  useEffect(() => {
    loadConversations();
  }, [apiKey]);

  const loadConversations = async () => {
    try {
      const fetchedConversations = await fetchConversations(apiKey);
      setConversations(fetchedConversations);
    } catch (error) {
      setChatError(error.message);
      console.error('Failed to load conversations:', error);
    }
  };

  const loadConversation = async (conversationId) => {
    try {
      const { messages } = await fetchConversation(apiKey, conversationId);
      setMessages(messages.map(msg => ({ ...msg, id: Date.now() + Math.random(), sender: 'user', content: msg.user, botContent: msg.bot })));
      setCurrentConversationId(conversationId);
      setShowHistory(false);
    } catch (error) {
      setChatError(error.message);
      console.error('Failed to load conversation:', error);
    }
  };

  const handleSendMessage = (messageText) => {
    if (!messageText.trim()) return;

    const userMessage = { id: Date.now(), sender: 'user', content: messageText };
    setMessages((prevMessages) => [...prevMessages, userMessage]);
    setIsLoadingResponse(true);
    setChatError('');

    let accumulatedContent = '';
    let botMessageId = Date.now() + 1;
    let currentSources = [];
    let initialMessageAdded = false;

    const onNewMessage = (data) => {
      if (data.type === 'text') {
        accumulatedContent += data.content;
        setMessages((prevMessages) => {
          const existingBotMessageIndex = prevMessages.findIndex(msg => msg.id === botMessageId);
          if (existingBotMessageIndex > -1) {
            const updatedMessages = [...prevMessages];
            updatedMessages[existingBotMessageIndex] = {
              ...updatedMessages[existingBotMessageIndex],
              content: accumulatedContent,
              isStreaming: true,
            };
            return updatedMessages;
          } else if (!initialMessageAdded) {
            initialMessageAdded = true;
            return [...prevMessages, { id: botMessageId, sender: 'bot', content: accumulatedContent, isStreaming: true }];
          }
          return prevMessages;
        });
      } else if (data.type === 'metadata' && data.source_documents) {
        currentSources = data.source_documents.map(doc => ({ title: doc.metadata?.title || doc.metadata?.source || 'N/A', url: doc.metadata?.url || '#' }));
        setMessages((prevMessages) => {
          const existingBotMessageIndex = prevMessages.findIndex(msg => msg.id === botMessageId);
          if (existingBotMessageIndex > -1) {
            const updatedMessages = [...prevMessages];
            updatedMessages[existingBotMessageIndex] = {
              ...updatedMessages[existingBotMessageIndex],
              sources: currentSources,
            };
            return updatedMessages;
          }
          return prevMessages;
        });
      } else if (data.type === 'end') {
        setMessages((prevMessages) => {
          return prevMessages.map(msg => msg.id === botMessageId ? { ...msg, isStreaming: false, conversation_id: data.conversation_id || currentConversationId } : msg);
        });
        setIsLoadingResponse(false);
        setCurrentConversationId(data.conversation_id || currentConversationId);
        loadConversations();
      } else if (data.type === 'error') {
        setChatError(data.message || 'Ocurrió un error en la respuesta del bot.');
        setIsLoadingResponse(false);
        setMessages((prevMessages) => prevMessages.filter(msg => msg.id !== botMessageId || !msg.isStreaming));
      }
    };

    const onComplete = () => {
      setIsLoadingResponse(false);
      loadConversations();
    };

    const onError = (errorData) => {
      setChatError(errorData.message || 'Error de comunicación con el servicio de IA.');
      setIsLoadingResponse(false);
      setMessages((prevMessages) => prevMessages.filter(msg => msg.id !== botMessageId || !msg.isStreaming));
    };

    streamChatResponse(apiKey, messageText, mode, currentConversationId, onNewMessage, onComplete, onError);
  };

  const handleSelectConversation = (conv) => {
    loadConversation(conv.conversation_id);
  };

  const handleDeleteConversation = async (conversationId) => {
    try {
      await deleteConversation(apiKey, conversationId);
      loadConversations();
      if (currentConversationId === conversationId) {
        setCurrentConversationId(null);
        setMessages([]);
      }
    } catch (error) {
      setChatError(error.message);
      console.error('Failed to delete conversation:', error);
    }
  };

  return (
    <div className="flex flex-col h-[90vh] w-full max-w-4xl mx-auto p-4 bg-gray-800 border border-green-700 rounded-lg shadow-lg relative">
      <button
        onClick={onLogout}
        className="absolute top-4 right-4 bg-red-700 hover:bg-red-600 text-white font-bold py-1 px-3 rounded text-sm"
      >
        Logout
      </button>

      <button
        onClick={() => setShowHistory(!showHistory)}
        className="absolute top-4 left-4 bg-blue-700 hover:bg-blue-600 text-white font-bold py-1 px-3 rounded text-sm"
      >
        {showHistory ? 'Ocultar Historial' : 'Mostrar Historial'}
      </button>

      <div className="flex flex-grow overflow-hidden mt-12">
        {showHistory && (
          <aside className="w-1/4 p-4 border-r border-green-700 overflow-y-auto">
            <History conversations={conversations} onSelect={handleSelectConversation} onDelete={handleDeleteConversation} currentConversationId={currentConversationId} />
          </aside>
        )}

        <main className={`flex flex-col flex-grow p-4 ${showHistory ? 'w-3/4' : 'w-full'}`}>
          <ModeSelector selectedMode={mode} onModeChange={setMode} />

          <div className="flex-grow overflow-y-auto space-y-4 p-2 rounded bg-gray-900 border border-green-700 mb-4 custom-scrollbar">
            {messages.map((msg) => (
              <Message key={msg.id} message={msg} />
            ))}
            {isLoadingResponse && (
              <div className="flex justify-start">
                <div className="bg-gray-700 text-green-400 p-3 rounded-lg max-w-[75%] animate-pulse">
                  <p>Pensando...</p>
                </div>
              </div>
            )}
            <div ref={messagesEndRef} />
          </div>

          {chatError && <p className="text-red-400 text-sm mb-2">{chatError}</p>}

          <ChatInput onSendMessage={handleSendMessage} isLoading={isLoadingResponse} />
        </main>
      </div>
    </div>
  );
}

export default Chat;