const AUTH_SERVICE_URL = 'http://auth-service.local/api/auth/validate'; // Usar el dominio de Traefik

export const validateApiKey = async (apiKey) => {
  try {
    const response = await fetch(AUTH_SERVICE_URL, {
      method: 'GET',
      headers: {
        'X-API-Key': apiKey,
        'Content-Type': 'text/plain', // Aunque es un GET, especificar para claridad
      },
    });

    if (response.ok) {
      const text = await response.text();
      return text === 'Valid API Key';
    } else {
      // Manejar 401 u otros códigos de error
      console.error(`Error ${response.status}: ${await response.text()}`);
      return false;
    }
  } catch (error) {
    console.error('Error de red o CORS al validar API Key:', error);
    throw new Error('Problema de conexión con el servicio de autenticación.');
  }
};

export const getStoredApiKey = () => {
  return localStorage.getItem('api_key');
};

export const setStoredApiKey = (apiKey) => {
  localStorage.setItem('api_key', apiKey);
};