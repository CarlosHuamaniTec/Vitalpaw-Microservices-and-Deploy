Documentación del Sistema de IA Actual

Este documento describe la arquitectura y funcionalidad de tu sistema de IA local, construido con Ollama, Qdrant y FastAPI, orquestado con Podman Compose.
Visión General del Servicio

Tu sistema es un Servidor Local de IA con RAG (Retrieval-Augmented Generation). Su propósito principal es interactuar con modelos de lenguaje grandes (LLMs) proporcionados por Ollama y gestionar una base de conocimiento documental utilizando una base de datos vectorial Qdrant. Esto permite que el sistema responda a consultas basándose en documentos cargados, además de recurrir a conocimientos generales del ámbito del software cuando la documentación específica es insuficiente.
Componentes Principales

El sistema está compuesto por tres servicios principales, cada uno encapsulado en un contenedor y orquestado mediante Podman Compose:

    ollama-gpu (Servicio Ollama)

        Propósito: Aloja y ejecuta los modelos de lenguaje (LLM) y los modelos de embedding (incrustación). Es el componente que realiza el procesamiento de lenguaje natural y la generación de respuestas.

        Modelos Configurados:

            LLM: phi4-mini:3.8b (para la generación de texto).

            Modelo de Embedding: bge-m3:567m (para convertir texto en vectores numéricos).

        Recursos: Está configurado para aprovechar la GPU de NVIDIA del host, lo cual es fundamental para el rendimiento en operaciones de IA.

        Acceso: Expuesto en el puerto 11434 dentro del contenedor, mapeado al puerto 11440 en el host.

        Persistencia: Utiliza un volumen (ollama_data) para almacenar los modelos descargados, evitando descargas repetidas.

    qdrant (Base de Datos Vectorial)

        Propósito: Almacena los embeddings (vectores numéricos) generados a partir de tus documentos. Permite realizar búsquedas de similitud ultrarrápidas para encontrar los fragmentos de texto más relevantes para una consulta.

        Persistencia: Utiliza un volumen (qdrant_data) para almacenar de forma persistente los datos vectoriales y sus índices.

        Acceso: Expuesto en el puerto 6333 (API principal) y 6334 (Interfaz de Usuario/Dashboard) dentro del contenedor, mapeados a los mismos puertos en el host.

    backend-ollama-api (API FastAPI)

        Propósito: Actúa como la interfaz de la aplicación. Gestiona la interacción entre el usuario, los documentos y los servicios de Ollama y Qdrant. Es el punto de entrada para la ingestión de documentos y las consultas RAG.

        Endpoints Clave:

            /ingest-doc/ (POST): Recibe archivos Markdown. Procesa el documento dividiéndolo en fragmentos, generando embeddings para cada fragmento, y almacenándolos en Qdrant. Esta función incluye lógica para reemplazar los fragmentos de un documento existente si se ingiere un archivo con el mismo nombre, garantizando que la base de datos se mantenga actualizada y limpia.

            /rag-query/ (POST): Recibe una consulta de texto. Busca en Qdrant los fragmentos de documento más relevantes. Pasa estos fragmentos como contexto al LLM de Ollama para generar una respuesta.

            / (GET): Un endpoint de salud básico para verificar si el servicio está funcionando.

        Dependencias: Depende de los servicios ollama-gpu y qdrant, asegurando que estén operativos antes de que el backend inicie.

        Disponibilidad: Expuesto en el puerto 8000.

Flujo de Operación (RAG) Detallado

    Ingestión de Documentos:

        Un usuario envía un archivo Markdown (ej., documento_tecnico.md) al endpoint /ingest-doc/.

        El backend-ollama-api asigna el nombre del archivo (documento_tecnico.md) como un document_id único para el documento.

        El contenido del archivo se lee y se divide en fragmentos (o "chunks") utilizando RecursiveCharacterTextSplitter.

        Manejo de Actualizaciones: Antes de añadir nuevos fragmentos, el sistema verifica si ya existen fragmentos asociados con el mismo document_id en Qdrant. Si existen, estos fragmentos antiguos son eliminados para asegurar que solo la versión más reciente del documento esté presente en la base de datos vectorial.

        Cada fragmento se envía al servicio ollama-gpu para ser convertido en un vector numérico (embedding) usando el modelo bge-m3:567m.

        Los nuevos embeddings, junto con sus metadatos (como document_id y chunk_index), se almacenan en la colección especificada de Qdrant (por defecto, default_docs).

    Realización de Consultas (RAG):

        Un usuario envía una pregunta al endpoint /rag-query/.

        El sistema genera un embedding de la pregunta del usuario.

        Realiza una búsqueda de similitud en la base de datos Qdrant para encontrar los 8 fragmentos de documento (k=8) más relevantes a la pregunta.

        Estos fragmentos se combinan para formar un "contexto" que se pasa al LLM (phi4-mini).

        Inteligencia del Prompt: La plantilla del prompt ha sido diseñada para guiar al LLM:

            Prioriza el uso de la información del Contexto proporcionado (es decir, la documentación).

            Si la documentación es limitada o no contiene la respuesta directa, el LLM tiene la libertad de utilizar su conocimiento general sobre el desarrollo de software, arquitectura de sistemas, metodologías y herramientas de la industria.

            Se le instruye explícitamente a mantener las respuestas relevantes al dominio del software y a indicar si una pregunta está completamente fuera de su ámbito.

        El LLM genera una respuesta basada en este contexto y sus conocimientos, que luego se devuelve al usuario junto con los metadatos de los documentos fuente utilizados.

Consideraciones para Producción (Nivel Académico)

Tu setup actual está muy bien preparado para un proyecto académico con fines de demostración y uso limitado (ej. 3 sesiones concurrentes).

    Robustez: Los health checks, políticas de reinicio y la persistencia de datos (volúmenes) contribuyen a un sistema robusto.

    Rendimiento: El uso de GPU para Ollama y una base de datos vectorial optimizada como Qdrant proporcionan un buen rendimiento para la escala esperada.

    Flexibilidad del Asistente: El prompt engineering ajustado permite que el asistente sea útil tanto con la documentación específica como con preguntas generales de software, sin desviarse del tema.

    Manejo de Documentación Múltiple: La lógica de ingestión permite subir múltiples archivos Markdown a la misma colección (default_docs) y actualizar documentos individuales por nombre de archivo, lo cual es eficiente para organizar el conocimiento.