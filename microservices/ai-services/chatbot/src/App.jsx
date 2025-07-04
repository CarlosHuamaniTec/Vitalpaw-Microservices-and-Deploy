import React, { useState, useEffect } from 'react';
import Chat from './components/Chat';
import AuthForm from './components/AuthForm';
import { validateApiKey } from './auth';

function App() {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [apiKey, setApiKey] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [authError, setAuthError] = useState('');

  useEffect(() => {
    const storedApiKey = localStorage.getItem('api_key');
    if (storedApiKey) {
      setApiKey(storedApiKey);
      handleAuthSubmit(storedApiKey, false);
    } else {
      setIsLoading(false);
    }
  }, []);

  const handleAuthSubmit = async (key, save = true) => {
    setIsLoading(true);
    setAuthError('');
    try {
      const isValid = await validateApiKey(key);
      if (isValid) {
        if (save) {
          localStorage.setItem('api_key', key);
        }
        setApiKey(key);
        setIsAuthenticated(true);
      } else {
        setAuthError('API Key inválida. Por favor, verifica tu clave.');
        setIsAuthenticated(false);
      }
    } catch (error) {
      console.error('Error durante la validación de API Key:', error);
      setAuthError('No se pudo conectar al servicio de autenticación. Intenta de nuevo más tarde.');
      setIsAuthenticated(false);
    } finally {
      setIsLoading(false);
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('api_key');
    setApiKey('');
    setIsAuthenticated(false);
    setAuthError('');
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-gray-900 text-green-400">
        <p className="text-xl">Cargando...</p>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-900 flex flex-col items-center justify-center p-4"> {/* Centrado y fondo oscuro */}
      {isAuthenticated ? (
        <Chat apiKey={apiKey} onLogout={handleLogout} />
      ) : (
        <AuthForm onSubmit={handleAuthSubmit} error={authError} />
      )}
    </div>
  );
}

export default App;