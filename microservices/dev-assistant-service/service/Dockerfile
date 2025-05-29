FROM python:3.10-slim

WORKDIR /app

# Copiar archivos de requisitos y wheels
COPY requirements.txt .
COPY wheels /wheels

# Instalar dependencias desde wheels si existen, de lo contrario desde internet
RUN pip install --no-cache-dir --find-links=/wheels -r requirements.txt || pip install --no-cache-dir -r requirements.txt

# Copiar el resto del código
COPY src/ .
COPY models/ .

# Comando para ejecutar el servicio
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8001"]