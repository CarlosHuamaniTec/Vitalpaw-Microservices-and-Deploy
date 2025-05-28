package com.vitalpaw.sensordata.service;

import com.vitalpaw.sensordata.model.SensorData;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;

@Service
public class SensorDataService {
    private final MqttClient mqttClient;
    private final WebClient webClient;
    @Value("${vet.ai.service.url}")
    private String vetAiServiceUrl;

    public SensorDataService(MqttClient mqttClient, WebClient.Builder webClientBuilder) throws Exception {
        this.mqttClient = mqttClient;
        this.webClient = webClientBuilder.build();
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
        // Parsear el payload (simplificado, asumimos JSON)
        SensorData data = parseSensorData(payload);
        sendToVetAiService(data);
    }

    private SensorData parseSensorData(String payload) {
        // Simulación: asumimos un formato JSON simple
        String[] parts = payload.split(",");
        SensorData data = new SensorData();
        data.setPetName(parts[0].split(":")[1].trim());
        data.setTemperature(Float.parseFloat(parts[1].split(":")[1].trim()));
        data.setHeartRate(Integer.parseInt(parts[2].split(":")[1].trim()));
        data.setTimestamp(LocalDateTime.now());
        return data;
    }

    private void sendToVetAiService(SensorData data) {
        String question = String.format("Analiza estos datos: temperatura=%f°C, heartRate=%d lpm para %s",
                data.getTemperature(), data.getHeartRate(), data.getPetName());
        webClient.post()
                .uri(vetAiServiceUrl + "/vet/query")
                .bodyValue(Map.of("question", question))
                .retrieve()
                .bodyToMono(String.class)
                .subscribe(response -> System.out.println("Response from Vet AI: " + response));
    }
}