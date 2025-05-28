package com.vitalpaw.sensordata.config;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class MqttConfig {
    @Value("${mqtt.broker}")
    private String mqttBroker;

    @Value("${mqtt.client.id}")
    private String clientId;

    @Value("${mqtt.username}")
    private String mqttUsername;

    @Value("${mqtt.password}")
    private String mqttPassword;

    @Bean
    public MqttClient mqttClient() throws Exception {
        MqttClient client = new MqttClient(mqttBroker, clientId);
        client.connect();
        return client;
    }
}