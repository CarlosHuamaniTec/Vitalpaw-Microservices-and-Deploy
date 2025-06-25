package com.vitalpaw.sensoralertservice.dto;

import lombok.Data;

@Data
public class SensorDataResponseDTO {
    private float temperature;
    private int pulse;
    private String status;
}