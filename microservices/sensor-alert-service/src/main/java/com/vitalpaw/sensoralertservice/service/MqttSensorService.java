package com.vitalpaw.sensoralert.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.vitalpaw.sensoralert.dto.SensorDataDTO;
import com.vitalpaw.sensoralert.model.Alert;
import com.vitalpaw.sensoralert.model.Breed;
import com.vitalpaw.sensoralert.model.Pet;
import com.vitalpaw.sensoralert.model.PetDevice;
import com.vitalpaw.sensoralert.repository.AlertRepository;
import com.vitalpaw.sensoralert.repository.PetDeviceRepository;
import jakarta.annotation.PostConstruct;
import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class MqttSensorService {
    private static final Logger logger = LoggerFactory.getLogger(MqttSensorService.class);

    @Value("${mqtt.broker.url}")
    private String broker;

    @Value("${mqtt.topic}")
    private String topic;

    @Value("${mqtt.username}")
    private String username;

    @Value("${mqtt.password}")
    private String password;

    @Value("${mqtt.client.id}")
    private String clientId;

    @Autowired
    private SimpMessagingTemplate template;

    @Autowired
    private PetDeviceRepository petDeviceRepository;

    @Autowired
    private AlertRepository alertRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        try {
            MqttClient client = new MqttClient(broker, clientId);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setUserName(username);
            connOpts.setPassword(password.toCharArray());
            connOpts.setAutomaticReconnect(true);
            client.connect(connOpts);

            client.subscribe(topic, (t, msg) -> {
                String payload = new String(msg.getPayload());
                logger.info("Mensaje MQTT recibido en topic {}: {}", t, payload);

                try {
                    SensorDataDTO sensorData = objectMapper.readValue(payload, SensorDataDTO.class);
                    PetDevice petDevice = petDeviceRepository.findByDeviceId(sensorData.getDeviceId())
                            .orElse(null);

                    if (petDevice != null) {
                        Pet pet = petDevice.getPet();
                        Breed breed = pet.getBreed();

                        if (breed != null) {
                            // Verificar rangos
                            boolean outOfRange = sensorData.getTemperature() < breed.getMinTemperature() ||
                                    sensorData.getTemperature() > breed.getMaxTemperature() ||
                                    sensorData.getPulse() < breed.getMinHeartRate() ||
                                    sensorData.getPulse() > breed.getMaxHeartRate();

                            if (outOfRange) {
                                // Crear y guardar alerta
                                Alert alert = new Alert();
                                alert.setPet(pet);
                                alert.setMessage(String.format("Valores fuera de rango: Temp=%.1f, Pulso=%d",
                                        sensorData.getTemperature(), sensorData.getPulse()));
                                alert.setType("sensor_alert");
                                alert.setSeverity("high");
                                alert.setPulse(sensorData.getPulse());
                                alert.setTemperature(sensorData.getTemperature());
                                alertRepository.save(alert);
                                logger.info("Alerta guardada para mascota {}: {}", pet.getId(), alert.getMessage());

                                // Enviar notificación FCM
                                String fcmToken = pet.getOwner().getFcmToken();
                                if (fcmToken != null && !fcmToken.isEmpty()) {
                                    Message fcmMessage = Message.builder()
                                            .setToken(fcmToken)
                                            .putData("title", "Alerta de Salud - " + pet.getName())
                                            .putData("body", alert.getMessage())
                                            .build();
                                    FirebaseMessaging.getInstance().send(fcmMessage);
                                    logger.info("Notificación FCM enviada a: {}", fcmToken);
                                } else {
                                    logger.warn("No se encontró fcmToken para el usuario {}", pet.getOwner().getId());
                                }
                            }
                        } else {
                            logger.warn("No se encontró raza para la mascota {}", pet.getId());
                        }

                        // Enviar datos al frontend vía WebSocket
                        template.convertAndSend("/topic/sensores/" + pet.getId(), sensorData);
                        logger.debug("Datos enviados a WebSocket para mascota {}", pet.getId());
                    } else {
                        logger.warn("No se encontró dispositivo con ID {}", sensorData.getDeviceId());
                    }
                } catch (Exception e) {
                    logger.error("Error al procesar mensaje MQTT: {}", e.getMessage(), e);
                }
            });

            logger.info("Suscrito al topic MQTT: {}", topic);
        } catch (MqttException e) {
            logger.error("Error al conectar al broker MQTT: {}", e.getMessage(), e);
            throw new RuntimeException("No se pudo conectar al broker MQTT", e);
        }
    }
}