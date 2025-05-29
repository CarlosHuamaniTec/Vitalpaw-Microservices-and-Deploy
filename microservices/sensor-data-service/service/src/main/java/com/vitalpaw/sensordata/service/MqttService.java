package com.vitalpaw.sensordataservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vitalpaw.sensordataservice.dto.SensorDataDTO;
import com.vitalpaw.sensordataservice.model.MedicalAlert;
import com.vitalpaw.sensordataservice.model.SensorData;
import com.vitalpaw.sensordataservice.repository.MedicalAlertRepository;
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
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class MqttService {

    @Value("${mqtt.broker}")
    private String broker;

    @Value("${mqtt.client.id}")
    private String clientId;

    @Autowired
    private SensorDataRepository sensorDataRepository;

    @Autowired
    private MedicalAlertRepository medicalAlertRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public final ConcurrentLinkedQueue<SensorDataDTO> dataQueue = new ConcurrentLinkedQueue<>();

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    private final RestTemplate restTemplate = new RestTemplate();

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
    public MqttPahoMessageDrivenChannelAdapter inbound(MqttPahoClientFactory mqttClientFactory) {
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(clientId + "_inbound", mqttClientFactory, "vitalpaw/sensors/+");
        adapter.setOutputChannel(mqttInputChannel());
        adapter.setQos(1);
        adapter.setConverter(new DefaultPahoMessageConverter());
        return adapter;
    }

    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void handleMessage(Message<String> message) {
        try {
            String topic = message.getHeaders().get("mqtt_receivedTopic", String.class);
            String payload = message.getPayload();

            String sensorId = topic.split("/")[2];
            ObjectMapper mapper = new ObjectMapper();
            SensorDataDTO sensorDataDTO = mapper.readValue(payload, SensorDataDTO.class);

            if (!sensorDataDTO.getSensorId().equals(sensorId)) {
                System.err.println("SensorId mismatch: topic=" + sensorId + ", payload=" + sensorDataDTO.getSensorId());
                return;
            }

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
                    executorService.submit(() -> processSensorData(dataDTO));
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
        Long petId = getPetIdBySensorId(dataDTO.getSensorId());
        if (petId == null) {
            System.err.println("No pet found for sensorId: " + dataDTO.getSensorId());
            return;
        }

        SensorData sensorData = new SensorData();
        sensorData.setSensorId(dataDTO.getSensorId());
        sensorData.setPetId(petId);
        sensorData.setTemperature(dataDTO.getTemperature());
        sensorData.setHeartRate(dataDTO.getHeartRate());
        sensorData.setX(dataDTO.getX());
        sensorData.setY(dataDTO.getY());
        sensorData.setZ(dataDTO.getZ());
        sensorData.setBreed(dataDTO.getBreed());
        sensorData.setTimestamp(dataDTO.getTimestamp());

        sensorDataRepository.save(sensorData);
        redisTemplate.opsForValue().set("sensor:" + sensorData.getId(), sensorData);

        // Guardar alertas médicas
        saveMedicalAlerts(sensorData, petId);

        String movementState = calculateMovementState(sensorData.getX(), sensorData.getY(), sensorData.getZ());
        if ("Inmovilidad".equals(movementState)) {
            notificationService.sendNotification("Alerta de Inmovilidad",
                    "Mascota " + petId + " está inmóvil");
        }

        double tempLimit = getTempLimitByBreed(sensorData.getBreed());
        int heartRateLimit = getHeartRateLimitByBreed(sensorData.getBreed());
        if (sensorData.getTemperature() > tempLimit || sensorData.getHeartRate() > heartRateLimit) {
            notificationService.sendNotification("Alerta de Salud",
                    "Mascota " + petId + " supera límites: Temp=" + sensorData.getTemperature() + "°C, HR=" + sensorData.getHeartRate());
        }
    }

    private Long getPetIdBySensorId(String sensorId) {
        String url = "http://localhost:8080/users/sensors/sensorId/" + sensorId;
        try {
            Response response = restTemplate.getForObject(url, Response.class);
            if (response != null) {
                return response.getPetId();
            }
        } catch (Exception e) {
            System.err.println("Error al consultar petId para sensorId " + sensorId + ": " + e.getMessage());
        }
        return null;
    }

    private void saveMedicalAlerts(SensorData sensorData, Long petId) {
        LocalDateTime now = LocalDateTime.now();
        String movementState = calculateMovementState(sensorData.getX(), sensorData.getY(), sensorData.getZ());
        if ("Inmovilidad".equals(movementState)) {
            MedicalAlert alert = new MedicalAlert();
            alert.setPetId(petId);
            alert.setAlertType("Inmovilidad");
            alert.setMessage("Mascota " + petId + " está inmóvil");
            alert.setTimestamp(now);
            medicalAlertRepository.save(alert);
        }

        double tempLimit = getTempLimitByBreed(sensorData.getBreed());
        int heartRateLimit = getHeartRateLimitByBreed(sensorData.getBreed());
        if (sensorData.getTemperature() > tempLimit || sensorData.getHeartRate() > heartRateLimit) {
            MedicalAlert alert = new MedicalAlert();
            alert.setPetId(petId);
            alert.setAlertType("Salud");
            alert.setMessage("Mascota " + petId + " supera límites: Temp=" + sensorData.getTemperature() + "°C, HR=" + sensorData.getHeartRate());
            alert.setTimestamp(now);
            medicalAlertRepository.save(alert);
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

    // Método para obtener datos para vet-ai-service (sin enviar aún)
    public void prepareVetAiData(Long petId, double currentTemperature, int currentHeartRate) {
        executorService.submit(() -> {
            // Obtener historial de alertas de la última semana
            LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);
            List<MedicalAlert> alerts = medicalAlertRepository.findByPetIdAndTimestampAfter(petId, oneWeekAgo);

            // Aquí podrías preparar un objeto DTO con los datos
            VetAiData data = new VetAiData();
            data.setPetId(petId);
            data.setAlerts(alerts);
            data.setCurrentTemperature(currentTemperature);
            data.setCurrentHeartRate(currentHeartRate);

            // Guardar en Redis o una cola para envío futuro
            redisTemplate.opsForValue().set("vet-ai-data:" + petId, data);
        });
    }

    // Clase auxiliar para datos de vet-ai-service
    private static class VetAiData {
        private Long petId;
        private List<MedicalAlert> alerts;
        private double currentTemperature;
        private int currentHeartRate;

        // Getters y setters
        public Long getPetId() { return petId; }
        public void setPetId(Long petId) { this.petId = petId; }
        public List<MedicalAlert> getAlerts() { return alerts; }
        public void setAlerts(List<MedicalAlert> alerts) { this.alerts = alerts; }
        public double getCurrentTemperature() { return currentTemperature; }
        public void setCurrentTemperature(double currentTemperature) { this.currentTemperature = currentTemperature; }
        public int getCurrentHeartRate() { return currentHeartRate; }
        public void setCurrentHeartRate(int currentHeartRate) { this.currentHeartRate = currentHeartRate; }
    }

    // Clase auxiliar para la respuesta de user-service
    private static class Response {
        private Long petId;

        public Long getPetId() { return petId; }
        public void setPetId(Long petId) { this.petId = petId; }
    }
}