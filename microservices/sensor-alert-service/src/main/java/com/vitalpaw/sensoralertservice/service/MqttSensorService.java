package com.vitalpaw.sensoralertservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.vitalpaw.sensoralertservice.dto.Esp32SensorDataDTO; // Nuevo DTO para los datos del ESP32
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

import java.time.LocalDateTime;
import java.util.Optional; // Importación para Optional

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

    @Value("${mqtt.topic}")
    private String mqttTopic;

    @Value("${app.sensor.thresholds.immobile}")
    private float immobileThreshold;

    @Value("${app.sensor.thresholds.fall}")
    private float fallThreshold;

    @Value("${app.sensor.thresholds.maxTemperature}")
    private float maxTemperature;

    @Value("${app.sensor.thresholds.maxHeartRate}")
    private int maxHeartRate;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private PetDeviceRepository petDeviceRepository;

    @Autowired
    private AlertRepository alertRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MqttClient mqttClient;

    @PostConstruct
    public void init() {
        try {
            mqttClient = new MqttClient(broker, clientId);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setUserName(username);
            connOpts.setPassword(password.toCharArray());
            connOpts.setAutomaticReconnect(true);
            connOpts.setCleanSession(true); // Limpia la sesión en cada reconexión
            mqttClient.connect(connOpts);

            mqttClient.subscribe(mqttTopic, (t, msg) -> processMessage(t, msg));
            logger.info("Suscrito al topic MQTT: {}", mqttTopic);

            logger.info("Conexión al broker MQTT establecida: {}", broker);
        } catch (MqttException e) {
            logger.error("Error al conectar al broker MQTT: {}", e.getMessage(), e);
            // Considera no lanzar RuntimeException aquí para que el servicio Spring Boot se inicie
            // y pueda intentar reconectar, pero loggear el error.
        }
    }

    private void processMessage(String topic, MqttMessage msg) {
        String payload = new String(msg.getPayload());
        logger.info("Mensaje recibido en el topic MQTT {}: {}", topic, payload);

        try {
            Esp32SensorDataDTO esp32Data = objectMapper.readValue(payload, Esp32SensorDataDTO.class);

            // --- Importante: Asignación de dispositivo/mascota ---
            // Tu ESP32 NO envía un 'deviceId'. Para la demo, lo estamos hardcodeando aquí.
            // Para una solución robusta, el ESP32 DEBERÍA incluir un identificador único en su JSON.
            String fixedDeviceId = "ESP32_VITALPAW_001"; // ID Fijo para la demo

            Optional<PetDevice> optionalPetDevice = petDeviceRepository.findByDeviceId(fixedDeviceId);
            PetDevice petDevice = optionalPetDevice.orElseGet(() -> {
                logger.warn("PetDevice con ID '{}' no encontrado. Intentando crear uno de ejemplo...", fixedDeviceId);
                // Si no existe, intenta asociarlo a la primera mascota existente para la demo
                // O deberías tener una forma de registrar dispositivos en tu backend/frontend
                List<Pet> allPets = petDeviceRepository.findAll().stream().map(PetDevice::getPet).toList();
                if (allPets.isEmpty()) {
                    logger.error("No hay mascotas registradas en la base de datos para asociar el dispositivo.");
                    return null; // No podemos continuar sin una mascota
                }
                Pet defaultPet = allPets.get(0); // Tomamos la primera mascota encontrada
                PetDevice newDevice = new PetDevice();
                newDevice.setDeviceId(fixedDeviceId);
                newDevice.setPet(defaultPet);
                newDevice.setIsActive(true);
                return petDeviceRepository.save(newDevice);
            });

            if (petDevice == null) {
                logger.error("No se pudo obtener/crear un PetDevice para procesar los datos. Abortando.");
                return;
            }

            Pet pet = petDevice.getPet();
            if (pet == null) {
                logger.error("El PetDevice '{}' no está asociado a ninguna mascota.", fixedDeviceId);
                return;
            }

            Breed breed = pet.getBreed(); // Necesitas una raza para los umbrales

            float temperature = esp32Data.getTemperatura_celsius();
            // El ESP32 solo envía 'ecg_raw', no un pulso calculado. Lo usamos como pulso para la demo.
            int pulse = esp32Data.getEcg_raw();
            String movimientoEstado = esp32Data.getMovimiento(); // "Sin movimiento", "En movimiento", "MPU6050 error lectura"

            // --- Lógica de Alertas ---
            String alertMessage = "";
            boolean isAlert = false;

            if (breed != null) {
                if (temperature < breed.getMinTemperature() || temperature > breed.getMaxTemperature()) {
                    isAlert = true;
                    alertMessage += String.format("Temp. fuera de rango (%.1fC). ", temperature);
                }
                // Si el pulso (ecg_raw) es un valor que se puede comparar con un rango de pulso de la raza
                if (pulse < breed.getMinHeartRate() || pulse > breed.getMaxHeartRate()) {
                    isAlert = true;
                    alertMessage += String.format("Pulso fuera de rango (%d BPM). ", pulse);
                }
            } else {
                logger.warn("La mascota {} no tiene una raza asociada. No se aplicarán umbrales específicos de raza.", pet.getId());
                // Puedes aplicar umbrales generales si no hay raza
                if (temperature > maxTemperature) { // Usa el umbral general del application.yml
                    isAlert = true;
                    alertMessage += String.format("Temp. alta (%.1fC). ", temperature);
                }
                if (pulse > maxHeartRate) { // Usa el umbral general del application.yml
                    isAlert = true;
                    alertMessage += String.format("Pulso alto (%d BPM). ", pulse);
                }
            }

            // Lógica para detectar "Caída" basada en el string del ESP32
            if ("Caído".equalsIgnoreCase(movimientoEstado)) {
                isAlert = true;
                alertMessage += "¡Posible caída detectada! ";
            }

            if (isAlert) {
                Alert alert = new Alert();
                alert.setPet(pet);
                alert.setMessage(alertMessage.trim());
                alert.setType("sensor_alert");
                alert.setPulse(pulse);
                alert.setTemperature(temperature);
                alert.setTimestamp(LocalDateTime.now());
                alertRepository.save(alert);
                logger.info("Alerta guardada para la mascota {}: {}", pet.getId(), alert.getMessage());

                // Enviar notificación FCM
                String fcmToken = pet.getOwner() != null ? pet.getOwner().getFcmToken() : null;
                if (fcmToken != null && !fcmToken.isEmpty()) {
                    try {
                        Message fcmMessage = Message.builder()
                                .setToken(fcmToken)
                                .putData("title", "Alerta de VitalPaw - " + pet.getName())
                                .putData("body", alert.getMessage())
                                .build();
                        FirebaseMessaging.getInstance().send(fcmMessage);
                        logger.info("Notificación FCM enviada a: {}", fcmToken);
                    } catch (FirebaseMessagingException e) {
                        logger.error("Error al enviar notificación FCM a {}: {}", fcmToken, e.getMessage(), e);
                    }
                } else {
                    logger.warn("No se encontró fcmToken para el dueño de la mascota {}.", pet.getName());
                }
            }

            // Enviar datos al frontend vía WebSocket
            SensorDataResponseDTO responseDTO = new SensorDataResponseDTO();
            responseDTO.setDeviceId(petDevice.getDeviceId()); // Incluye el deviceId
            responseDTO.setPetId(pet.getId()); // Incluye el petId
            responseDTO.setTemperature(temperature);
            responseDTO.setPulse(pulse);
            responseDTO.setStatus(movimientoEstado); // 'Sin movimiento', 'En movimiento', 'Caído', etc.
            
            // Envío al tópico WebSocket específico de la mascota
            messagingTemplate.convertAndSend("/topic/sensores/" + pet.getId(), responseDTO);
            logger.debug("Datos enviados a WebSocket para la mascota {}: Temp=%.1f, Pulso=%d, Estado=%s",
                    pet.getId(), temperature, pulse, movimientoEstado);

        } catch (MqttException e) {
            logger.error("Error MQTT al procesar mensaje: {}", e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error al procesar mensaje MQTT: {}", e.getMessage(), e);
        }
    }
}