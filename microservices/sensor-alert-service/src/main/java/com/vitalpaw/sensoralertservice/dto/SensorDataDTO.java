package com.vitalpaw.sensoralert.dto;

import lombok.Data;

@Data
public class SensorDataDTO {
    private String deviceId;
    private float temperature;
    private int pulse;
}