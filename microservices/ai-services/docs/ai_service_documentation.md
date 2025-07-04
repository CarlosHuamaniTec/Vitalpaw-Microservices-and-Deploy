# Documentación del Servicio de Inteligencia Artificial

## Introducción

El sistema de Inteligencia Artificial (IA) está diseñado para proporcionar capacidades de procesamiento de lenguaje natural y búsqueda semántica dentro de una arquitectura de microservicios. Este sistema permite ingerir documentación técnica en formato Markdown, generar embeddings vectoriales, y responder consultas contextuales mediante Retrieval-Augmented Generation (RAG). Se integra con otros microservicios, como el servicio de sensor, para soportar aplicaciones de monitoreo de mascotas en tiempo real. Los componentes principales son `ollama-gpu`, `qdrant`, y `backend-ollama-api`, que trabajan juntos para procesar y analizar información técnica.

## Propósito del Servicio de IA

El servicio de IA tiene como objetivo:
- **Procesamiento de documentación**: Ingerir y vectorizar documentos Markdown para habilitar búsquedas semánticas.
- **Consultas inteligentes**: Responder preguntas técnicas basadas en documentación mediante RAG, utilizando modelos de lenguaje y embeddings.
- **Integración con microservicios**: Complementar servicios como el de sensor, que analiza datos en tiempo real, con capacidades de IA para consultas contextuales.
- **Escalabilidad**: Operar en contenedores gestionados por Podman, con soporte para GPUs y almacenamiento persistente.

Para detalles sobre el servicio de sensor, consulte `sensor_alert_service_documentation.md`. Para información sobre otros microservicios, vea `core_service_documentation.md`.

## Componentes del Sistema de IA

### Servicio Ollama-GPU

El servicio `ollama-gpu` ejecuta modelos de lenguaje y embeddings en un contenedor optimizado para GPUs NVIDIA.

- **Función**: Proporciona el modelo de lenguaje `phi4-mini:3.8b` para generar respuestas y el modelo de embedding `bge-m3:567m` para vectorizar texto.
- **Configuración**:
  - Puerto: `11440` (mapeado a `11434` internamente).
  - Variables de entorno: `LLM_MODEL=phi4-mini:3.8b`, `EMBEDDING_MODEL=bge-m3:567m`.
  - Volumen: `ollama_data` para persistencia de modelos.
  - Healthcheck: Verifica el endpoint `/api/tags`.
- **Dependencias**: Requiere soporte GPU mediante `NVIDIA_VISIBLE_DEVICES=all`.

Este servicio responde a solicitudes HTTP para generar embeddings y respuestas de texto, integrándose con `backend-ollama-api`.

### Servicio Qdrant

El servicio `qdrant` es una base de datos vectorial que almacena embeddings de documentos para búsquedas semánticas.

- **Función**: Gestiona colecciones de vectores (por ejemplo, `default_docs`) para recuperar documentos relevantes basados en similitud coseno.
- **Configuración**:
  - Puertos: `6333` (REST) y `6334` (gRPC).
  - Volumen: `qdrant_data` para almacenamiento persistente.
  - Healthcheck: Verifica el endpoint `/readyz` con la respuesta `all shards are ready`.
- **Dependencias**: Interactúa con `backend-ollama-api` para ingesta y consultas.

Qdrant crea colecciones dinámicamente con un tamaño de vector de 1024 (determinado por `bge-m3:567m`) y usa la métrica de distancia coseno.

### Servicio Backend-Ollama-API

El servicio `backend-ollama-api` es una API FastAPI que coordina la ingesta de documentos y las consultas RAG.

- **Función**:
  - **Ingesta**: Procesa archivos Markdown en `/app/docs`, genera embeddings, y los almacena en Qdrant.
  - **Consultas RAG**: Recupera documentos relevantes de Qdrant y genera respuestas contextuales usando Ollama.
- **Configuración**:
  - Puerto: `8000`.
  - Variables de entorno: `OLLAMA_GPU_HOST`, `QDRANT_HOST`, `API_KEY`, `LLM_MODEL`, `EMBEDDING_MODEL`.
  - Volumen: Monta `./docs` como `/app/docs` para documentos Markdown.
  - Healthcheck: Verifica el endpoint `/health`.
- **Endpoints**:
  - `GET /health`: Verifica el estado del servicio.
  - `POST /ingest-doc`: Ingiere un documento Markdown.
  - `POST /rag-query`: Procesa consultas RAG, con soporte para streaming.
- **Dependencias**: Requiere `ollama-gpu` y `qdrant`.

El backend usa `langchain-ollama` y `langchain-qdrant` para interactuar con los modelos y la base vectorial.

## Flujo de Trabajo del Sistema de IA

1. **Ingesta de documentos**:
   - Al iniciar, `backend-ollama-api` escanea `/app/docs` y procesa archivos Markdown.
   - Los documentos se dividen en fragmentos (`chunk_size=500`, `chunk_overlap=100`) usando `RecursiveCharacterTextSplitter`.
   - Cada fragmento se vectoriza con `bge-m3:567m` (vía `ollama-gpu`) y se almacena en Qdrant (`default_docs`).
2. **Consultas RAG**:
   - Un cliente envía una consulta al endpoint `/rag-query`.
   - El backend vectoriza la consulta, recupera hasta 8 fragmentos relevantes de Qdrant, y construye un prompt con el contexto.
   - El prompt se envía a `phi4-mini:3.8b` para generar una respuesta, que se devuelve como JSON o flujo SSE.
3. **Integración**:
   - El sistema de IA puede responder consultas sobre la documentación técnica, complementando el análisis en tiempo real del servicio de sensor (ver `sensor_alert_service_documentation.md`).

## Integración con Otros Microservicios

El sistema de IA se integra con:
- **Servicio de sensor**: Procesa datos MQTT, genera alertas, y usa MariaDB y FCM (ver `sensor_alert_service_documentation.md`).
- **Servicios core**: Gestionan la infraestructura de microservicios (ver `core_service_documentation.md`).
- **Otros**: Configuraciones de red, nodos, y puertos (ver `network_ports_guidelines.md`, `node_management.md`, `docker_swarm_config.md`, `frp_setup.md`).

El sistema de IA no procesa datos de sensores directamente, pero puede responder consultas sobre su funcionamiento basadas en la documentación.

## Configuración Técnica

- **Podman Compose**: Gestiona los servicios `ollama-gpu`, `qdrant`, y `backend-ollama-api` (ver `compose.yml`).
- **Dependencias**:
  - `fastapi==0.115.2`, `uvicorn==0.32.0`, `langchain-ollama==0.2.0`, `langchain-qdrant==0.2.0`, `qdrant-client==1.12.1`, entre otras (ver `requirements.txt`).
- **Volúmenes**:
  - `ollama_data`: Persistencia de modelos Ollama.
  - `qdrant_data`: Persistencia de colecciones Qdrant.
  - `./docs:/app/docs`: Documentos Markdown locales.
- **Seguridad**: Autenticación mediante `API_KEY` en `backend-ollama-api`.

## Mejores Prácticas para Consultas

- **Consultas claras**: Use preguntas específicas, por ejemplo, "What is the purpose of the AI service?" o "How does Qdrant store vectors?".
- **Fragmentación**: Los documentos se dividen en fragmentos de 500 caracteres para optimizar la búsqueda semántica.
- **Actualización de documentos**: Suba nuevos documentos Markdown al endpoint `/ingest-doc` para mantener la base de conocimiento actualizada.

## Referencias

- `sensor_alert_service_documentation.md`: Detalles del servicio de sensor.
- `core_service_documentation.md`: Documentación de los servicios core.
- `network_ports_guidelines.md`, `node_management.md`, `docker_swarm_config.md`, `frp_setup.md`: Configuraciones relacionadas.
- Repositorio del proyecto: [VitalPaw Microservices](https://github.com/vitalpaw/microservices).