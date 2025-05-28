package com.vitalpaw.mainclient.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.vitalpaw.mainclient.config.MqttConfig;
import com.vitalpaw.mainclient.model.BiometricData;
import com.vitalpaw.mainclient.model.Pet;
import com.vitalpaw.mainclient.model.Thresholds;
import com.vitalpaw.mainclient.repository.PetRepository;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class MqttService {
    private final MqttAsyncClient client;
    private final ObjectMapper objectMapper;
    private final FirebaseMessaging firebaseMessaging;
    private final RedisTemplate<String, String> redisTemplate;
    private final PetRepository petRepository;
    private final RestTemplate restTemplate;
    private final MqttConfig mqttConfig;

    @Autowired
    public MqttService(ObjectMapper objectMapper, FirebaseMessaging firebaseMessaging, RedisTemplate<String, String> redisTemplate, PetRepository petRepository, MqttConfig mqttConfig) throws MqttException {
        this.objectMapper = objectMapper;
        this.firebaseMessaging = firebaseMessaging;
        this.redisTemplate = redisTemplate;
        this.petRepository = petRepository;
        this.restTemplate = new RestTemplate();
        this.mqttConfig = mqttConfig;
        this.client = new MqttAsyncClient(mqttConfig.getBroker(), "mainclient");
        client.setCallback(new MqttCallback() {
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                redisTemplate.opsForList().leftPush("biometric_queue", new String(message.getPayload()));
            }
            public void connectionLost(Throwable cause) {}
            public void deliveryComplete(IMqttDeliveryToken token) {}
        });
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(mqttConfig.getUsername());
        options.setPassword(mqttConfig.getPassword().toCharArray());
        client.connect(options).waitForCompletion();
        client.subscribe("pet/biometric/+", 1);
    }

    @Scheduled(fixedRate = 100)
    public void processBiometricQueue() throws Exception {
        String payload = redisTemplate.opsForList().rightPop("biometric_queue");
        while (payload != null) {
            BiometricData data = objectMapper.readValue(payload, BiometricData.class);
            String petId = data.getPetId();
            String breed = getPetBreed(petId);
            Thresholds thresholds = getThresholds(breed);
            List<String> alerts = checkSymptoms(data, thresholds, petId);
            if (!alerts.isEmpty()) {
                sendNotification(petId, alerts);
            }
            payload = redisTemplate.opsForList().rightPop("biometric_queue");
        }
    }

    private String getPetBreed(String petId) {
        Pet pet = petRepository.findById(Long.parseLong(petId)).orElse(null);
        return pet != null ? pet.getBreed() : "Labrador";
    }

    private Thresholds getThresholds(String breed) {
        try {
            ResponseEntity<Thresholds> response = restTemplate.getForEntity(
                "http://django:8000/api/thresholds/?breed=" + breed,
                Thresholds.class
            );
            return response.getBody();
        } catch (Exception e) {
            return new Thresholds(breed, 60.0, 120.0, 36.5, 39.5);
        }
    }

    private List<String> checkSymptoms(BiometricData data, Thresholds thresholds, String petId) {
        List<String> alerts = new ArrayList<>();

        // Fiebre
        if (data.getTemperature() > thresholds.getMaxTemperature()) {
            alerts.add("Fiebre detectada: Temperatura " + data.getTemperature() + "°C");
        }
        // Hipotermia
        if (data.getTemperature() < thresholds.getMinTemperature()) {
            alerts.add("Hipotermia detectada: Temperatura " + data.getTemperature() + "°C");
        }
        // Taquicardia
        if (data.getHeartRate() > thresholds.getMaxHeartRate()) {
            alerts.add("Taquicardia detectada: Frecuencia cardíaca " + data.getHeartRate() + " lpm");
        }
        // Bradicardia
        if (data.getHeartRate() < thresholds.getMinHeartRate()) {
            alerts.add("Bradicardia detectada: Frecuencia cardíaca " + data.getHeartRate() + " lpm");
        }

        // Movimiento: Caída y Letargo
        double magnitude = Math.sqrt(
            data.getX() * data.getX() +
            data.getY() * data.getY() +
            data.getZ() * data.getZ()
        );

        // Caída: Cambio brusco en magnitud (>20 m/s²) seguido de inactividad
        String lastMagnitudeKey = "last_magnitude:" + petId;
        String lastMagnitudeStr = redisTemplate.opsForValue().get(lastMagnitudeKey);
        double lastMagnitude = lastMagnitudeStr != null ? Double.parseDouble(lastMagnitudeStr) : magnitude;
        redisTemplate.opsForValue().set(lastMagnitudeKey, String.valueOf(magnitude));

        if (magnitude > 20.0 && lastMagnitude < 5.0) {
            // Cambio brusco seguido de baja actividad
            alerts.add("Caída detectada: Movimiento anormal");
        }

        // Inmovilidad: Magnitud baja (<1 m/s²) durante 5 minutos
        String movementHistoryKey = "movement_history:" + petId;
        redisTemplate.opsForList().leftPush(movementHistoryKey, String.valueOf(magnitude));
        redisTemplate.opsForList().trim(movementHistoryKey, 0, 2999); // 5 min a 10 Hz = 3000 muestras
        redisTemplate.expire(movementHistoryKey, 5, TimeUnit.MINUTES);

        List<String> movementHistory = redisTemplate.opsForList().range(movementHistoryKey, 0, -1);
        if (movementHistory != null && movementHistory.size() >= 3000) {
            boolean isLethargic = movementHistory.stream()
                .mapToDouble(Double::parseDouble)
                .allMatch(m -> m < 1.0);
            if (isLethargic) {
                alerts.add("Inmovilidad detectada: Baja actividad durante 5 minutos");
                redisTemplate.delete(movementHistoryKey); // Evitar notificaciones repetidas
            }
        }

        return alerts;
    }

    private void sendNotification(String petId, List<String> alerts) {
        String deviceToken = redisTemplate.opsForValue().get("device_token:" + petId);
        if (deviceToken != null) {
            Message message = Message.builder()
                .setNotification(Notification.builder()
                    .setTitle("Alerta de Salud - Mascota " + petId)
                    .setBody(String.join("; ", alerts))
                    .build())
                .setToken(deviceToken)
                .build();
            try {
                firebaseMessaging.send(message);
            } catch (Exception e) {
                // Log error
            }
        }
    }
}