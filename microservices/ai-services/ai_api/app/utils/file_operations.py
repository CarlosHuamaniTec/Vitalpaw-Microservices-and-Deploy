import os
from fastapi import HTTPException, status
import logging

logger = logging.getLogger(__name__)

def read_markdown_file(file_path: str) -> str:
    logger.info(f"Attempting to read Markdown file: {file_path}")
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            markdown_text = f.read()
        if not markdown_text.strip():
            logger.warning(f"Empty Markdown file detected: {file_path}.")
            raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="The Markdown file is empty or contains only whitespace.")
        return markdown_text
    except FileNotFoundError:
        logger.error(f"File not found at specified path: {file_path}", exc_info=True)
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=f"File not found at {file_path}.")
    except Exception as e:
        logger.error(f"Error reading Markdown file {file_path}: {e}", exc_info=True)
        raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail=f"An unexpected error occurred while reading the file: {e}")