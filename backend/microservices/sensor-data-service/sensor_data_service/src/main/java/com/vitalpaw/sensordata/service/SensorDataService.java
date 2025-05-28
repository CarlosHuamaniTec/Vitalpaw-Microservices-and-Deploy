package com.vitalpaw.sensordata.service;

import com.vitalpaw.sensordata.model.SensorData;
import com.vitalpaw.sensordata.model.Thresholds;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SensorDataService {
    private final MqttClient mqttClient;
    private final WebClient webClient;
    private final RestTemplate restTemplate;
    private final AlertService alertService;
    @Value("${vet.ai.service.url}")
    private String vetAiServiceUrl;
    @Value("${admin.service.url}")
    private String adminServiceUrl;

    public SensorDataService(MqttClient mqttClient, WebClient.Builder webClientBuilder, AlertService alertService) throws Exception {
        this.mqttClient = mqttClient;
        this.webClient = webClientBuilder.build();
        this.restTemplate = new RestTemplate();
        this.alertService = alertService;
        setupMqttListener();
    }

    private void setupMqttListener() throws Exception {
        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                System.out.println("Connection lost: " + cause.getMessage());
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                String payload = new String(message.getPayload());
                processSensorData(payload);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                // No implementado
            }
        });
        mqttClient.connect();
        mqttClient.subscribe("vitalpaw/sensors");
    }

    public void processSensorData(String payload) {
        SensorData data = parseSensorData(payload);
        String breed = getPetBreed(data.getPetName());
        Thresholds thresholds = getThresholds(breed);
        List<String> alerts = checkSymptoms(data, thresholds);
        if (!alerts.isEmpty()) {
            alerts.forEach(alert -> alertService.saveAlertFromFirebase(alert, determineSeverity(alert)));
        }
        sendToVetAiService(data);
    }

    private SensorData parseSensorData(String payload) {
        // Simulación: asumimos un formato CSV simple
        String[] parts = payload.split(",");
        SensorData data = new SensorData();
        data.setPetName(parts[0].split(":")[1].trim());
        data.setTemperature(Float.parseFloat(parts[1].split(":")[1].trim()));
        data.setHeartRate(Integer.parseInt(parts[2].split(":")[1].trim()));

        // Extraer valores del giroscopio y calcular movimiento
        double x = Double.parseDouble(parts[3].split(":")[1].trim());
        double y = Double.parseDouble(parts[4].split(":")[1].trim());
        double z = Double.parseDouble(parts[5].split(":")[1].trim());
        data.setMovementState(calculateMovementState(x, y, z));

        data.setTimestamp(LocalDateTime.now());
        return data;
    }

    private String getPetBreed(String petName) {
        try {
            return restTemplate.getForObject(adminServiceUrl + "/api/pets?name=" + petName + "&field=breed", String.class);
        } catch (Exception e) {
            return "Labrador"; // Fallback
        }
    }

    private Thresholds getThresholds(String breed) {
        try {
            return restTemplate.getForObject(adminServiceUrl + "/api/thresholds?breed=" + breed, Thresholds.class);
        } catch (Exception e) {
            return new Thresholds(breed, 60.0, 120.0, 36.5, 39.5); // Fallback
        }
    }

    private List<String> checkSymptoms(SensorData data, Thresholds thresholds) {
        List<String> alerts = new ArrayList<>();

        // Síntomas basados en raza
        if (data.getHeartRate() > thresholds.getMaxHeartRate()) {
            alerts.add("Taquicardia: Frecuencia cardíaca " + data.getHeartRate() + " lpm");
        }
        if (data.getHeartRate() < thresholds.getMinHeartRate()) {
            alerts.add("Bradicardia: Frecuencia cardíaca " + data.getHeartRate() + " lpm");
        }
        if (data.getTemperature() > thresholds.getMaxTemperature()) {
            alerts.add("Fiebre: Temperatura " + data.getTemperature() + "°C");
        }
        if (data.getTemperature() < thresholds.getMinTemperature()) {
            alerts.add("Hipotermia: Temperatura " + data.getTemperature() + "°C");
        }

        // Movimiento
        String movementState = data.getMovementState();
        if ("Inmovilidad".equals(movementState)) {
            alerts.add("Inmovilidad detectada");
        }

        return alerts;
    }

    private String determineSeverity(String alert) {
        if (alert.contains("Taquicardia") || alert.contains("Fiebre")) {
            return "HIGH";
        } else if (alert.contains("Bradicardia") || alert.contains("Hipotermia")) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }

    private String calculateMovementState(double x, double y, double z) {
        double magnitude = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));
        if (magnitude > 1.0) {
            return "En movimiento";
        } else if (magnitude > 0.1) {
            return "Movimiento disminuido";
        } else {
            return "Inmovilidad";
        }
    }

    private void sendToVetAiService(SensorData data) {
        String question = String.format("Analiza estos datos: temperatura=%f°C, heartRate=%d lpm, movimiento=%s para %s",
                data.getTemperature(), data.getHeartRate(), data.getMovementState(), data.getPetName());
        webClient.post()
                .uri(vetAiServiceUrl + "/vet/query")
                .bodyValue(Map.of("question", question))
                .retrieve()
                .bodyToMono(String.class)
                .subscribe(response -> System.out.println("Response from Vet AI: " + response));
    }
}