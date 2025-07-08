package com.vitalpaw.sensoralertservice.dto;

import lombok.Data;

@Data
public class SensorDataDTO {
    private String sensorID;
    private float temp;
    private int pulse;
    private float x;
    private float y;
    private float z;
}