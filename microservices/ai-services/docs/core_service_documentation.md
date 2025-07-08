# Documentación del Core Service

## Introducción
El `core-service` es el componente central de la aplicación VitalPaw, diseñado para gestionar las entidades principales del sistema, como usuarios, mascotas, razas, dispositivos y alertas. Este servicio expone una API RESTful que permite a otros componentes y clientes interactuar con estas entidades de manera eficiente.

## Arquitectura
- **Framework**: Spring Boot 3.2.5  
- **Lenguaje**: Java 17  
- **Base de datos**: MariaDB  
- **Autenticación**: API Key  
- **Documentación de API**: Swagger/OpenAPI  

El servicio sigue una arquitectura de microservicios, con un diseño modular que separa las responsabilidades en controladores, servicios y repositorios, utilizando el patrón MVC.

## Tecnologías Utilizadas
- **Spring Boot Starter Web**: Para la creación de la API RESTful.  
- **Spring Boot Starter Data JPA**: Para la interacción con la base de datos mediante Hibernate.  
- **Spring Boot Starter Security**: Para la autenticación basada en API Key.  
- **Spring Boot Starter Mail**: Para el envío de correos electrónicos (confirmación, restablecimiento de contraseña).  
- **MariaDB JDBC Driver**: Conector para la base de datos MariaDB.  
- **Swagger/OpenAPI**: Herramienta para generar documentación interactiva de la API.  

## Endpoints Principales

### Usuarios
- **`POST /api/users`**  
  Crea un nuevo usuario en el sistema.  
  - **Request Body**: JSON con datos como email, nombre y contraseña.  
  - **Respuesta**: ID del usuario creado y token de confirmación.  
- **`GET /api/users/{id}`**  
  Obtiene la información de un usuario por su ID.  
- **`PUT /api/users/{id}`**  
  Actualiza los datos de un usuario existente.  
- **`DELETE /api/users/{id}`**  
  Elimina un usuario del sistema.  
- **`GET /api/users/confirm/{token}`**  
  Confirma la cuenta de un usuario utilizando un token enviado por correo.  
- **`POST /api/users/password-reset/request`**  
  Solicita un enlace para restablecer la contraseña.  
- **`POST /api/users/password-reset`**  
  Restablece la contraseña utilizando un token.  
- **`POST /api/users/{id}/change-password`**  
  Permite a un usuario cambiar su contraseña actual.  
- **`PUT /api/users/{id}/fcm-token`**  
  Actualiza el token de Firebase Cloud Messaging (FCM) para notificaciones push.  

### Mascotas
- **`POST /api/pets`**  
  Registra una nueva mascota asociada a un usuario.  
- **`GET /api/pets/{id}`**  
  Obtiene los detalles de una mascota por su ID.  
- **`PUT /api/pets/{id}`**  
  Actualiza la información de una mascota.  
- **`POST /api/pets/{id}/photo`**  
  Sube una foto para la mascota, almacenada en el servidor.  

### Razas
- **`POST /api/breeds`**  
  Crea una nueva raza en el sistema.  
- **`GET /api/breeds/{id}`**  
  Obtiene los detalles de una raza específica.  
- **`GET /api/breeds`**  
  Lista todas las razas disponibles.  

### Dispositivos
- **`POST /api/devices`**  
  Registra un nuevo dispositivo asociado a una mascota.  
- **`GET /api/devices/{id}`**  
  Obtiene los detalles de un dispositivo por su ID.  
- **`GET /api/devices/device/{deviceId}`**  
  Busca un dispositivo por su identificador único.  
- **`PUT /api/devices/{id}`**  
  Actualiza la información de un dispositivo.  
- **`DELETE /api/devices/{id}`**  
  Elimina un dispositivo del sistema.  

### Alertas
- **`GET /api/alerts/pet/{petId}`**  
  Lista todas las alertas asociadas a una mascota específica.  
- **`DELETE /api/alerts/{alertId}`**  
  Elimina una alerta por su ID.  

## Flujo de Datos
1. **Registro de Usuario**: Un usuario envía una solicitud a `POST /api/users`, recibiendo un token de confirmación por correo.  
2. **Confirmación**: El usuario usa `GET /api/users/confirm/{token}` para activar su cuenta.  
3. **Gestión de Mascotas**: Los usuarios crean y gestionan mascotas con los endpoints correspondientes.  
4. **Subida de Fotos**: Las fotos se suben mediante `POST /api/pets/{id}/photo` y se almacenan en el servidor.  
5. **Asociación de Dispositivos**: Los dispositivos se registran y vinculan a mascotas específicas.  
6. **Gestión de Alertas**: Las alertas generadas por otros servicios se consultan o eliminan desde este servicio.  

## Seguridad
- **Autenticación**: Todos los endpoints protegidos requieren una API Key enviada en el header `X-API-Key`.  
- **Excepciones**: Los endpoints públicos como confirmación de cuenta y restablecimiento de contraseña no requieren autenticación.  
- **Validación**: Uso de filtros y anotaciones para garantizar la integridad de los datos en las solicitudes.  

## Configuración
- **Base de Datos**: Configurada en `application.yml` con variables de entorno:  
  - Host: `DB_HOST`  
  - Puerto: `DB_PORT`  
  - Nombre: `DB_NAME`  
  - Usuario: `DB_USER`  
  - Contraseña: `DB_PASSWORD`  
- **Correo Electrónico**: Usa Gmail SMTP para enviar correos, configurado con credenciales en variables de entorno.  
- **Almacenamiento**: Las imágenes de mascotas se guardan en `/app/images/pets` dentro del contenedor.  

## Despliegue
- **Docker**: El servicio se empaqueta en una imagen Docker que incluye la compilación del proyecto y su ejecución.  
- **Secrets**: Las claves sensibles (API Key, credenciales de correo) se gestionan mediante Docker secrets.  

## Mejores Prácticas
- **Validación**: Uso de DTOs con anotaciones de validación para entradas de datos.  
- **Excepciones**: Manejo global con `@ControllerAdvice` para respuestas uniformes.  
- **Transacciones**: Uso de `@Transactional` en operaciones críticas de base de datos.  
- **Documentación**: Swagger genera una interfaz interactiva para probar la API.