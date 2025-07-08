package com.vitalpaw.sensoralertservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vitalpaw.sensoralertservice.dto.SensorDataResponseDTO; // Usar el DTO correcto
import io.micrometer.core.instrument.Gauge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public SensorWebSocketHandler(io.micrometer.core.instrument.MeterRegistry meterRegistry) {
        // Métricas de conexiones WebSocket activas
        Gauge.builder("vitalpaw.websocket.connections", sessions, List::size)
                .description("Número de conexiones WebSocket activas")
                .register(meterRegistry);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // La lógica de validación de API Key ha sido eliminada.
        sessions.add(session);
        logger.info("Nueva conexión WebSocket establecida: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // Este servicio no espera mensajes entrantes desde el frontend vía WebSocket,
        // solo los envía. Puedes loguear si lo deseas.
        logger.debug("Mensaje WebSocket recibido de {}: {}", session.getId(), message.getPayload());
    }

    // Este método es llamado desde MqttSensorService para enviar datos al frontend
    // Ahora usa el DTO de respuesta para mantener la consistencia
    public void broadcastSensorData(SensorDataResponseDTO data) throws IOException {
        String jsonData = objectMapper.writeValueAsString(data);
        TextMessage message = new TextMessage(jsonData);
        logger.debug("Enviando datos de sensor para mascota {}: Temp=%.1f, Pulso=%d, Estado=%s",
                data.getPetId(), data.getTemperature(), data.getPulse(), data.getStatus());
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