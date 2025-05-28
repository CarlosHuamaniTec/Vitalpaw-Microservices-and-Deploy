package com.vitalpaw.sensordataservice.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SensorDataDTO {
    private Long petId;
    private double temperature;
    private int heartRate;
    private String movementState;
    private String breed;
    private LocalDateTime timestamp;
}