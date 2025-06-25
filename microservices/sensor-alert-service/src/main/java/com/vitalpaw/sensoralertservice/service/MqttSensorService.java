package com.vitalpaw.sensoralertservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.vitalpaw.sensoralertservice.dto.SensorDataDTO;
import com.vitalpaw.sensoralertservice.dto.SensorDataResponseDTO;
import com.vitalpaw.sensoralertservice.entity.Alert;
import com.vitalpaw.sensoralertservice.entity.Breed;
import com.vitalpaw.sensoralertservice.entity.Pet;
import com.vitalpaw.sensoralertservice.entity.PetDevice;
import com.vitalpaw.sensoralertservice.repository.AlertRepository;
import com.vitalpaw.sensoralertservice.repository.PetDeviceRepository;
import jakarta.annotation.PostConstruct;
import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MqttSensorService {
    private static final Logger logger = LoggerFactory.getLogger(MqttSensorService.class);

    @Value("${mqtt.broker.url}")
    private String broker;

    @Value("${mqtt.username}")
    private String username;

    @Value("${mqtt.password}")
    private String password;

    @Value("${mqtt.client.id}")
    private String clientId;

    @Value("${app.sensor.thresholds.immobile}")
    private float immobileThreshold;

    @Value("${app.sensor.thresholds.fall}")
    private float fallThreshold;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private PetDeviceRepository petDeviceRepository;

    @Autowired
    private AlertRepository alertRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MqttClient mqttClient;

    // Sensibilidad del MPU6050 para ±2g (16384 LSB/g)
    private static final float MPU6050_SENSITIVITY = 16384.0f;

    @PostConstruct
    public void init() {
        try {
            mqttClient = new MqttClient(broker, clientId);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setUserName(username);
            connOpts.setPassword(password.toCharArray());
            connOpts.setAutomaticReconnect(true);
            mqttClient.connect(connOpts);

            // Suscribirse a topics por deviceId
            List<PetDevice> devices = petDeviceRepository.findAll();
            for (PetDevice device : devices) {
                String topic = "vitalpaw/health/" + device.getDeviceId();
                mqttClient.subscribe(topic, (t, msg) -> processMessage(t, msg));
                logger.info("Suscrito al topic MQTT: {}", topic);
            }

            logger.info("Conexión al broker MQTT establecida: {}", broker);
        } catch (MqttException e) {
            logger.error("Error al conectar al broker MQTT: {}", e.getMessage(), e);
            throw new RuntimeException("No se pudo conectar al broker MQTT", e);
        }
    }

    private void processMessage(String topic, MqttMessage msg) {
        String payload = new String(msg.getPayload());
        logger.info("Mensaje recibido en el topic MQTT {}: {}", topic, payload);

        try {
            SensorDataDTO sensorData = objectMapper.readValue(payload, SensorDataDTO.class);
            String deviceId = sensorData.getSensorID();
            PetDevice petDevice = petDeviceRepository.findByDeviceId(deviceId)
                    .orElse(null);

            if (petDevice != null) {
                Pet pet = petDevice.getPet();
                Breed breed = pet.getBreed();

                if (breed != null) {
                    // Verificar rangos de temperatura y pulso
                    boolean outOfRange = sensorData.getTemp() < breed.getMinTemperature() ||
                            sensorData.getTemp() > breed.getMaxTemperature() ||
                            sensorData.getPulse() < breed.getMinHeartRate() ||
                            sensorData.getPulse() > breed.getMaxHeartRate();

                    if (outOfRange) {
                        // Crear y guardar alerta
                        Alert alert = new Alert();
                        alert.setPet(pet);
                        alert.setMessage(String.format("Valores fuera de rango: Temp=%.1f, Pulso=%d",
                                sensorData.getTemp(), sensorData.getPulse()));
                        alert.setType("sensor_alert");
                        alert.setPulse(sensorData.getPulse());
                        alert.setTemperature(sensorData.getTemp());
                        alertRepository.save(alert);
                        logger.info("Alerta guardada para la mascota {}: {}", pet.getId(), alert.getMessage());

                        // Enviar notificación FCM
                        String fcmToken = pet.getOwner().getFcmToken();
                        if (fcmToken != null && !fcmToken.isEmpty()) {
                            try {
                                Message fcmMessage = Message.builder()
                                        .setToken(fcmToken)
                                        .putData("title", "Alerta de Salud - " + pet.getName())
                                        .putData("body", alert.getMessage())
                                        .build();
                                FirebaseMessaging.getInstance().send(fcmMessage);
                                logger.info("Notificación FCM enviada a: {}", fcmToken);
                            } catch (FirebaseMessagingException e) {
                                logger.error("Error al enviar notificación FCM a {}: {}", fcmToken, e.getMessage(), e);
                            }
                        } else {
                            logger.warn("No se encontró fcmToken para el usuario {}", pet.getOwner().getId());
                        }
                    }

                    // Normalizar valores de X, Y, Z a g
                    float x_g = sensorData.getX() / MPU6050_SENSITIVITY;
                    float y_g = sensorData.getY() / MPU6050_SENSITIVITY;
                    float z_g = sensorData.getZ() / MPU6050_SENSITIVITY;

                    // Procesar estado de movimiento
                    String status = determineMovementStatus(x_g, y_g, z_g);

                    // Enviar datos al frontend vía WebSocket
                    SensorDataResponseDTO responseDTO = new SensorDataResponseDTO();
                    responseDTO.setTemperature(sensorData.getTemp());
                    responseDTO.setPulse(sensorData.getPulse());
                    responseDTO.setStatus(status);
                    messagingTemplate.convertAndSend("/topic/sensores/" + pet.getId(), responseDTO);
                    logger.debug("Datos enviados a WebSocket para la mascota {}: Temp=%.1f, Pulso=%d, Estado=%s",
                            pet.getId(), sensorData.getTemp(), sensorData.getPulse(), status);
                } else {
                    logger.warn("No se encontró raza para la mascota {}", pet.getId());
                }
            } else {
                logger.warn("No se encontró dispositivo con ID {}", deviceId);
            }
        } catch (Exception e) {
            logger.error("Error al procesar mensaje MQTT: {}", e.getMessage(), e);
        }
    }

    private String determineMovementStatus(float x, float y, float z) {
        double magnitude = Math.sqrt(x * x + y * y + z * z);
        if (magnitude <= immobileThreshold) {
            return "Inmóvil";
        } else if (magnitude >= fallThreshold) {
            return "Caído";
        } else {
            return "Activo";
        }
    }
}