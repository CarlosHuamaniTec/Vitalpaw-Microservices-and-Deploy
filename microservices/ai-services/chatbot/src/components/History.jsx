import React from 'react';

function History({ conversations, onSelect, onDelete, currentConversationId }) {
  return (
    <div>
      <h2 className="text-lg font-bold mb-4 text-green-400">Historial de Conversaciones</h2>
      {conversations.length === 0 ? (
        <p className="text-gray-500 text-sm">No hay conversaciones previas.</p>
      ) : (
        <ul className="space-y-2">
          {conversations.map((conv) => (
            <li
              key={conv.conversation_id}
              className={`flex justify-between items-center p-2 rounded-md transition-colors duration-200 border border-green-800
                ${currentConversationId === conv.conversation_id ? 'bg-green-900 border-l-4 border-green-500' : 'bg-gray-800 hover:bg-gray-700'}
              `}
            >
              <button
                onClick={() => onSelect(conv)}
                className="flex-1 text-left text-green-400 font-medium truncate"
                title={conv.name}
              >
                {conv.name || `Conversación ${conv.conversation_id.substring(0, 8)}...`}
              </button>
              <button
                onClick={() => onDelete(conv.conversation_id)}
                className="ml-2 p-1 text-red-500 hover:text-red-400"
                title="Eliminar conversación"
              >
                {/* SVG del icono de basura */}
                <svg xmlns="[http://www.w3.org/2000/svg](http://www.w3.org/2000/svg)" className="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                   <path fillRule="evenodd" d="M9 2a1 1 0 00-.894.553L7.382 4H4a1 1 0 000 2v10a2 2 0 002 2h8a2 2 0 002-2V6a1 1 0 100-2h-3.382l-.724-1.447A1 1 0 0011 2H9zM7 8a1 1 0 011-1h4a1 1 0 110 2H8a1 1 0 01-1-1zm6 3a1 1 0 100 2H8a1 1 0 100-2h5z" clipRule="evenodd" />
                </svg>
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

export default History;