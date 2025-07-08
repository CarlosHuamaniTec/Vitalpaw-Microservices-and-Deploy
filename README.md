# VitalPaw: Sistema de Gestión de Mascotas

El sistema de Vitalpaw esta conformado por una arquitectura de microservicios usando DTO en los microservicios. El sistema esta hecho para desplegarse en una VPS con IP pública asociada a un dominio.

---

## 🛠️ Tecnologías Principales

* **Backend:** Java 17, Spring Boot
* **Base de Datos:** MariaDB
* **Orquestación de Contenedores:** Docker Swarm
* **Proxy Inverso & Balanceador de Carga:** Traefik
* **Administración de Base de Datos:** PHPMyAdmin
* **Documentación de API:** Swagger (OpenAPI 3)
* **Contenedores:** Docker

---

## 🏗️ Microservicios

Este proyecto consta de dos microservicios Spring Boot, aunque solo uno está actualmente desplegado y funcional en el entorno descrito.

### 1. `core-service` (Desplegado y Funcional)

* **Propósito:** Este es el microservicio principal y funcional del sistema. Se encarga de la lógica de negocio central, incluyendo la gestión de razas de mascotas y la persistencia de datos relacionados.
* **Tecnología:** Desarrollado con Spring Boot.
* **API:** Expone una API RESTful para la interacción con los datos de mascotas. La documentación interactiva de esta API está disponible a través de Swagger UI.
* **Conexión a Datos:** Se conecta a una base de datos MariaDB.

### 2. `sensor-alert-service` (No Desplegado, Funcionalidad No Confirmada)

* **Propósito:** Este microservicio se incluye como una plantilla de diseño y funcionalidad, aunque no ha sido desplegado en el entorno actual y su operativa no ha sido completamente validada.
* **Funcionalidad Propuesta:** Está diseñado para la suscripción a tópicos MQTT, procesamiento de datos de sensores y el envío de alertas. Utiliza Firebase Admin SDK para posibles notificaciones y WebSockets para la comunicación en tiempo real con usuarios.
* **Referencia:** Este servicio se basó en conceptos explorados en el repositorio [backend_sPRINGBOOT](https://github.com/AlvarezDiego26/backend_sPRINGBOOT.git).

---

## 🌐 Servicios de Infraestructura (Desplegados con Docker Swarm)

Estos servicios complementan el ecosistema de VitalPaw, proporcionando la infraestructura necesaria para el despliegue y la gestión.

* **`mariadb`**: Servidor de base de datos relacional.
    * Imagen Docker: `mariadb:10.11.13-jammy`
* **`phpmyadmin`**: Interfaz web para la administración de la base de datos MariaDB.
    * Imagen Docker: `phpmyadmin:5.2.2-apache`
    * Accesible vía Traefik en la ruta `/admin/`.
* **`traefik`**: Proxy inverso, balanceador de carga y terminador SSL/TLS.
    * Imagen Docker: `traefik:v3.4.1`
    * Maneja el enrutamiento del tráfico a los servicios internos y provee certificados SSL/TLS con Let's Encrypt.
    * Su panel de control es accesible en la ruta `/dashboard`.

---

## 🚀 Despliegue en Docker Swarm

Este proyecto está configurado para un despliegue en un clúster de Docker Swarm.

### Prerrequisitos

* Un clúster de Docker Swarm inicializado (con roles de `manager` y `worker` configurados).
* Docker Engine instalado en todos los nodos del clúster.
* Acceso a un registro de Docker (como `161.132.38.142:5000`) donde estén alojadas las imágenes del `core-service` y `sensor-alert-service`.
* Un dominio configurado (e.g., `vitalpaw.example.com`) apuntando a los nodos del clúster de Swarm.

### Configuración de Secrets

Antes del despliegue, es necesario crear los siguientes Docker secrets:

* `db_password`: Contraseña para el usuario de la base de datos `vitalpaw`.
* `db_root_password`: Contraseña para el usuario `root` de MariaDB.
* `mail_password`: Contraseña para el servicio de envío de correos (utilizado por `core-service`).
* `api_key`: Clave API para autenticación (utilizada por `core-service`).
* `mqtt_password`: Contraseña para el broker MQTT (utilizada por `sensor-alert-service`, aunque no desplegado).

Ejemplo de creación de un secret:
```bash
echo "tu_contrasena_secreta" | docker secret create nombre_del_secret -
