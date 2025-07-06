FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn dependency:go-offline && mvn package -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/target/coreservice-0.0.1-SNAPSHOT.jar app.jar

ENV CORE_SERVICE_PORT=8080 \
    DB_PORT=3306 \
    DB_NAME=vitalpawdb \
    DB_USERNAME=vitalpaw \
    SPRING_JPA_HIBERNATE_DDL_AUTO=validate \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

EXPOSE ${CORE_SERVICE_PORT}

RUN useradd -m appuser && \
    mkdir -p /app/images/pets && \
    chown -R appuser:appuser /app/images

USER appuser

ENTRYPOINT ["sh", "-c", "\
    export DB_PASSWORD=$(cat /run/secrets/db_password) && \
    export MAIL_PASSWORD=$(cat /run/secrets/mail_password) && \
    export API_KEY=$(cat /run/secrets/api_key) && \
    exec java ${JAVA_OPTS} -Dserver.port=${CORE_SERVICE_PORT} -jar app.jar\
"]