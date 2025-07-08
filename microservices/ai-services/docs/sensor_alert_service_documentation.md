# Documentación del Sensor Alert Service

## Introducción
El `sensor-alert-service` es un microservicio especializado en procesar datos de sensores recibidos a través de MQTT, generar alertas cuando los valores exceden rangos normales y notificar a los usuarios mediante Firebase Cloud Messaging (FCM). Además, proporciona datos en tiempo real a través de WebSocket.

## Arquitectura
- **Framework**: Spring Boot 3.2.5  
- **Lenguaje**: Java 17  
- **Base de datos**: MariaDB (compartida con `core-service`)  
- **Mensajería**: MQTT (Eclipse Paho)  
- **Notificaciones**: Firebase Cloud Messaging (FCM)  
- **WebSocket**: Comunicación en tiempo real con clientes  

El servicio está diseñado para alta disponibilidad y procesamiento en tiempo real, integrándose con otros componentes del sistema.

## Tecnologías Utilizadas
- **Spring Boot Starter Web**: Base para el microservicio.  
- **Spring Boot Starter Data JPA**: Persistencia en MariaDB.  
- **Spring Boot Starter WebSocket**: Comunicación en tiempo real.  
- **Spring Boot Starter Data Redis**: Caché (opcional, configurado pero no utilizado explícitamente).  
- **Eclipse Paho MQTT Client**: Conexión al broker MQTT.  
- **Firebase Admin SDK**: Envío de notificaciones push.  
- **MariaDB JDBC Driver**: Conexión a la base de datos.  
- **Jackson Databind**: Serialización/deserialización de datos.  
- **SLF4J**: Logging de eventos y errores.  
- **Spring Boot Starter Actuator**: Monitoreo del servicio.  

## Componentes Principales

### MQTT Sensor Service
- **Función**: Escucha mensajes en un topic MQTT configurado.  
- **Procesamiento**:  
  1. Deserializa los datos recibidos en formato JSON.  
  2. Compara los valores con rangos normales definidos por raza (almacenados en la base de datos).  
  3. Genera alertas si los valores están fuera de rango.  
- **Acciones**:  
  - Almacena las alertas en MariaDB.  
  - Envía notificaciones push a través de FCM al dueño de la mascota.  
  - Transmite los datos a clientes WebSocket conectados.  

### Sensor WebSocket Handler
- **Función**: Gestiona conexiones WebSocket.  
- **Autenticación**: Verifica la API Key en la query string de la conexión.  
- **Transmisión**: Difunde datos de sensores en tiempo real a los clientes autenticados.  

## Flujo de Datos
1. **Recepción**: Los datos de sensores llegan al topic MQTT suscrito.  
2. **Procesamiento**: Se analizan los valores y se comparan con rangos predefinidos.  
3. **Alertas**: Si hay anomalías, se registra una alerta y se notifica al usuario vía FCM.  
4. **Tiempo Real**: Los datos procesados se envían a través de WebSocket a los clientes conectados.  

## Seguridad
- **WebSocket**: Requiere una API Key válida en la query string (`?apiKey=valor`).  
- **MQTT**: Autenticación configurada con usuario y contraseña en el broker.  
- **Datos Sensibles**: Gestionados mediante variables de entorno y Docker secrets.  

## Configuración
- **MQTT**: Configurado en `application.yml`:  
  - Broker URL: `MQTT_BROKER_URL`  
  - Usuario: `MQTT_USERNAME`  
  - Contraseña: `MQTT_PASSWORD`  
  - Client ID: `MQTT_CLIENT_ID`  
  - Topic: `MQTT_TOPIC`  
- **Firebase**: Inicializado con `firebase-adminsdk.json` cargado desde el contenedor.  
- **Base de Datos**: Igual que `core-service`, configurada con variables de entorno.  
- **Redis**: Configurado pero no utilizado explícitamente en el flujo principal.  

## Despliegue
- **Docker**: Empaquetado en una imagen Docker con dependencias incluidas.  
- **Secrets**: Credenciales y claves sensibles manejadas mediante Docker secrets.  

## Mejores Prácticas
- **Reconexión**: MQTT está configurado para reconectarse automáticamente ante fallos.  
- **Logging**: SLF4J registra eventos clave y errores para depuración.  
- **Errores**: Captura y manejo de excepciones en procesamiento MQTT y WebSocket.  
- **Concurrencia**: Uso de `CopyOnWriteArrayList` para gestionar sesiones WebSocket de forma segura en entornos multihilo.