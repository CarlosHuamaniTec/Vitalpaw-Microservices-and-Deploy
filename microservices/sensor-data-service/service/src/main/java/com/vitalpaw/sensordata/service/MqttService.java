package com.vitalpaw.sensordataservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vitalpaw.sensordataservice.dto.SensorDataDTO;
import com.vitalpaw.sensordataservice.model.SensorData;
import com.vitalpaw.sensordataservice.repository.SensorDataRepository;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.messaging.MessageChannel;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentLinkedQueue;

@Configuration
public class MqttService {

    @Value("${mqtt.broker}")
    private String broker;

    @Value("${mqtt.client.id}")
    private String clientId;

    @Autowired
    private SensorDataRepository sensorDataRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private final ConcurrentLinkedQueue<SensorDataDTO> dataQueue = new ConcurrentLinkedQueue<>();

    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[]{broker});
        options.setKeepAliveInterval(10);
        factory.setConnectionOptions(options);
        return factory;
    }

    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MqttPahoMessageDrivenChannelAdapter inbound() {
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(clientId, mqttClientFactory(), "vitalpaw/sensors");
        adapter.setOutputChannel(mqttInputChannel());
        adapter.setQos(1);
        return adapter;
    }

    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void handleMessage(String message) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            SensorDataDTO sensorDataDTO = mapper.readValue(message, SensorDataDTO.class);
            sensorDataDTO.setTimestamp(LocalDateTime.now());
            dataQueue.offer(sensorDataDTO);
        } catch (Exception e) {
            System.err.println("Error al procesar mensaje MQTT: " + e.getMessage());
        }
    }

    @Bean
    public Runnable processDataTask() {
        return () -> {
            while (true) {
                SensorDataDTO dataDTO = dataQueue.poll();
                if (dataDTO != null) {
                    processSensorData(dataDTO);
                }
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        };
    }

    private void processSensorData(SensorDataDTO dataDTO) {
        SensorData sensorData = new SensorData();
        sensorData.setPetId(dataDTO.getPetId());
        sensorData.setTemperature(dataDTO.getTemperature());
        sensorData.setHeartRate(dataDTO.getHeartRate());
        sensorData.setX(dataDTO.getX());
        sensorData.setY(dataDTO.getY());
        sensorData.setZ(dataDTO.getZ());
        sensorData.setBreed(dataDTO.getBreed());
        sensorData.setTimestamp(dataDTO.getTimestamp());

        sensorDataRepository.save(sensorData);
        redisTemplate.opsForValue().set("sensor:" + sensorData.getId(), sensorData);

        String movementState = calculateMovementState(sensorData.getX(), sensorData.getY(), sensorData.getZ());
        if ("Inmovilidad".equals(movementState)) {
            // Simulación: obtener token del usuario asociado al petId (necesitaría integración con user-service)
            String userToken = getUserTokenByPetId(sensorData.getPetId()); // Método a implementar
            if (userToken != null) {
                notificationService.sendNotification("Alerta de Inmovilidad",
                        sensorData.getPetId() + " está inmóvil", userToken);
            }
        }

        double tempLimit = getTempLimitByBreed(sensorData.getBreed());
        int heartRateLimit = getHeartRateLimitByBreed(sensorData.getBreed());
        if (sensorData.getTemperature() > tempLimit || sensorData.getHeartRate() > heartRateLimit) {
            String userToken = getUserTokenByPetId(sensorData.getPetId());
            if (userToken != null) {
                notificationService.sendNotification("Alerta de Salud",
                        sensorData.getPetId() + " supera límites: Temp=" + sensorData.getTemperature() + "°C, HR=" + sensorData.getHeartRate(),
                        userToken);
            }
        }
    }

    private String calculateMovementState(double x, double y, double z) {
        double magnitude = Math.sqrt(x * x + y * y + z * z);
        if (magnitude < 0.1) return "Inmovilidad";
        if (magnitude < 0.5) return "Poco Activo";
        return "Muy Activo";
    }

    private double getTempLimitByBreed(String breed) {
        return switch (breed != null ? breed.toLowerCase() : "default") {
            case "golden retriever" -> 39.0;
            case "labrador" -> 39.2;
            default -> 38.5;
        };
    }

    private int getHeartRateLimitByBreed(String breed) {
        return switch (breed != null ? breed.toLowerCase() : "default") {
            case "golden retriever" -> 130;
            case "labrador" -> 140;
            default -> 120;
        };
    }

    // Método placeholder: necesita integración con user-service para obtener el token
    private String getUserTokenByPetId(Long petId) {
        // Simulación: en producción, consulta user-service via api-gateway
        // Ejemplo: REST call a /users/pets/{petId}/owner/token
        return "dummy-token"; // Reemplazar con lógica real
    }
}