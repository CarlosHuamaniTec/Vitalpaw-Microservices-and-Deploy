import httpx
from fastapi import HTTPException, Depends, status, Request
from fastapi.security import APIKeyHeader
from slowapi.util import get_remote_address
from . import config
import logging

logger = logging.getLogger(__name__)

api_key_header = APIKeyHeader(name="X-API-Key", auto_error=False)

async def verify_api_key(request: Request, api_key: str = Depends(api_key_header)):
    if not api_key:
        logger.warning("Attempted access without API Key.")
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="API Key required.")
    try:
        response = await config.http_client_instance.get(
            f"{config.AUTH_SERVICE_URL}/validate",
            headers={"X-API-Key": api_key},
            timeout=5
        )
        if response.status_code != status.HTTP_200_OK:
            logger.warning(f"Invalid API Key or error from auth-service: {response.status_code}. Details: {response.text}")
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail=f"Invalid API Key. Auth service response: {response.text}"
            )
    except httpx.RequestError as e:
        logger.error(f"Could not connect to authentication service at {config.AUTH_SERVICE_URL}: {e}", exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail=f"Authentication service unavailable: {str(e)}"
        )
    except Exception as e:
        logger.error(f"An unexpected error occurred during API Key validation: {e}", exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"An internal server error occurred during authentication: {str(e)}"
        )
    return api_key