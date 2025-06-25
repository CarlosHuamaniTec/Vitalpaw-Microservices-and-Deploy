package com.vitalpaw.sensoralertservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vitalpaw.sensoralertservice.entity.SensorData;
import io.micrometer.core.instrument.Gauge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class SensorWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(SensorWebSocketHandler.class);
    private final CopyOnWriteArrayList<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Value("${api.key}")
    private String apiKey;

    public SensorWebSocketHandler(io.micrometer.core.instrument.MeterRegistry meterRegistry) {
        Gauge.builder("vitalpaw.websocket.connections", sessions, List::size)
                .description("Número de conexiones WebSocket activas")
                .register(meterRegistry);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String requestApiKey = extractApiKey(session);
        if (requestApiKey == null || !requestApiKey.equals(apiKey)) {
            session.close(CloseStatus.BAD_DATA.withReason("Clave API inválida o ausente"));
            return;
        }
        sessions.add(session);
        logger.info("Nueva conexión WebSocket establecida: {}", session.getId());
    }

    private String extractApiKey(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null && query.contains("apiKey=")) {
            return query.split("apiKey=")[1].split("&")[0];
        }
        return null;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        logger.debug("Mensaje WebSocket recibido de {}: {}", session.getId(), message.getPayload());
    }

    public void broadcastSensorData(SensorData data) throws IOException {
        String jsonData = objectMapper.writeValueAsString(data);
        TextMessage message = new TextMessage(jsonData);
        logger.debug("Enviando datos de sensor para dispositivo {}: Temp=%.1f, Pulso=%d, Estado=%s",
                data.getDeviceId(), data.getTemperature(), data.getPulse(), data.getStatus());
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(message);
                } catch (IOException e) {
                    logger.error("Error al enviar mensaje a la sesión {}: {}", session.getId(), e.getMessage());
                }
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        logger.info("Conexión WebSocket cerrada: {} con estado: {}", session.getId(), status);
    }
}