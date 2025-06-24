package com.vitalpaw.sensoralertservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vitalpaw.sensoralertservice.entity.SensorData;
import io.micrometer.core.instrument.MeterRegistry;
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

    public SensorWebSocketHandler(MeterRegistry meterRegistry) {
        Gauge.builder("vitalpaw.websocket.connections", sessions, List::size)
                .description("Number of active WebSocket connections")
                .register(meterRegistry);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String requestApiKey = extractApiKey(session);
        if (requestApiKey == null || !requestApiKey.equals(apiKey)) {
            session.close(CloseStatus.BAD_DATA.withReason("Invalid or missing API Key"));
            return;
        }
        sessions.add(session);
        logger.info("New WebSocket connection established: {}", session.getId());
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
        logger.debug("Received WebSocket message from {}: {}", session.getId(), message.getPayload());
    }

    public void broadcastSensorData(SensorData data) throws IOException {
        String jsonData = objectMapper.writeValueAsString(data);
        TextMessage message = new TextMessage(jsonData);
        logger.debug("Broadcasting sensor data for device {}: {}", data.getDeviceId(), jsonData);
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(message);
                } catch (IOException e) {
                    logger.error("Failed to send message to session {}: {}", session.getId(), e.getMessage());
                }
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        logger.info("WebSocket connection closed: {} with status {}", session.getId(), status);
    }
}