package com.vitalpaw.sensordata.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class SensorData {
    private String petName;
    private Float temperature;
    private Integer heartRate;
    private String movementState; // "En movimiento", "Movimiento disminuido", "Inmovilidad"
    private LocalDateTime timestamp;
}