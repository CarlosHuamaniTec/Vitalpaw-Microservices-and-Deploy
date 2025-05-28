package com.vitalpaw.sensordata.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// Esta clase no se guarda en la DB, solo se usa para procesar datos
@Getter
@Setter
public class SensorData {
    private String petName;
    private Float temperature;
    private Integer heartRate;
    private String movementState;
    private LocalDateTime timestamp;
}