import React, { useState } from 'react';

function ChatInput({ onSendMessage, isLoading }) {
  const [inputText, setInputText] = useState('');

  const handleSubmit = (e) => {
    e.preventDefault();
    if (inputText.trim()) {
      onSendMessage(inputText);
      setInputText('');
    }
  };

  return (
    <form onSubmit={handleSubmit} className="flex gap-2 mt-4">
      <input
        type="text"
        className="flex-1 p-2 border border-green-700 rounded-md bg-gray-700 text-green-400 placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-green-500"
        value={inputText}
        onChange={(e) => setInputText(e.target.value)}
        placeholder="vitalpaw@terminal:~$ " // Estilo de prompt
        disabled={isLoading}
      />
      <button
        type="submit"
        className="bg-green-700 hover:bg-green-600 text-black font-bold py-2 px-4 rounded-md disabled:opacity-50 disabled:cursor-not-allowed"
        disabled={isLoading}
      >
        Enviar
      </button>
    </form>
  );
}

export default ChatInput;