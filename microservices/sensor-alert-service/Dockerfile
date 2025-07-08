FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn dependency:go-offline && mvn package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/sensor-alert-service-0.0.1-SNAPSHOT.jar app.jar
COPY firebase-adminsdk.json /app/firebase-adminsdk.json

ENV SENSOR_ALERT_SERVICE_PORT=8084 \
    DB_PORT=3306 \
    DB_NAME=vitalpawdb \
    DB_USERNAME=vitalpaw \
    SPRING_JPA_HIBERNATE_DDL_AUTO=validate \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

EXPOSE ${SENSOR_ALERT_SERVICE_PORT}

RUN useradd -m appuser && chown -R appuser:appuser /app
USER appuser

ENTRYPOINT ["sh", "-c", "\
    export DB_PASSWORD=$(cat /run/secrets/db_password 2>/dev/null || echo '') && \
    export MQTT_PASSWORD=$(cat /run/secrets/mqtt_password 2>/dev/null || echo 'ricardo') && \
    export API_KEY=$(cat /run/secrets/api_key 2>/dev/null || echo '') && \
    exec java ${JAVA_OPTS} -Dserver.port=${SENSOR_ALERT_SERVICE_PORT} -jar app.jar\
"]