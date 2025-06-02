package com.vitalpaw.sensordataservice.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SensorDataDTO {
    private String sensorId; // Cambiado de petId a sensorId
    private double temperature;
    private int heartRate;
    private double x;
    private double y;
    private double z;
    private String breed;
    private LocalDateTime timestamp;
}