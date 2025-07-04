import React, { useState } from 'react';

function AuthForm({ onSubmit, error }) {
  const [inputKey, setInputKey] = useState('');

  const handleSubmit = (e) => {
    e.preventDefault();
    onSubmit(inputKey);
  };

  return (
    <div className="flex items-center justify-center min-h-screen bg-gray-900 p-4">
      <form onSubmit={handleSubmit} className="bg-gray-800 p-8 rounded-lg shadow-lg w-full max-w-sm border border-green-700"> {/* Fondo oscuro, borde verde */}
        <h2 className="text-2xl font-bold mb-6 text-center text-green-400">Ingresar API Key</h2>
        <div className="mb-4">
          <label htmlFor="apiKey" className="block text-green-400 text-sm font-bold mb-2">
            API Key:
          </label>
          <input
            type="password"
            id="apiKey"
            className="shadow appearance-none border border-green-700 rounded w-full py-2 px-3 bg-gray-700 text-green-400 leading-tight focus:outline-none focus:shadow-outline focus:border-green-500" // Colores de input
            value={inputKey}
            onChange={(e) => setInputKey(e.target.value)}
            required
          />
        </div>
        {error && <p className="text-red-400 text-xs italic mb-4">{error}</p>} {/* Rojo para errores */}
        <div className="flex items-center justify-center">
          <button
            type="submit"
            className="bg-green-700 hover:bg-green-600 text-black font-bold py-2 px-4 rounded focus:outline-none focus:shadow-outline" // Botón oscuro/negro con texto
          >
            Validar
          </button>
        </div>
      </form>
    </div>
  );
}

export default AuthForm;