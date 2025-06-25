package com.vitalpaw.sensoralertservice.entity;

import lombok.Data;

@Data
public class SensorData {
    private String deviceId;
    private Long petId;
    private String type;
    private Float temperature;
    private Integer heartRate;
    private Float xAxis;
    private Float yAxis;
    private Float zAxis;
}