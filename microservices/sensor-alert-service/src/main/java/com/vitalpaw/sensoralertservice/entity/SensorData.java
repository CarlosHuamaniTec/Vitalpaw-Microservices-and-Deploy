package com.vitalpaw.sensoralertservice.entity;

import lombok.Data;

@Data
public class SensorData {
    private String deviceId;
    private Long petId;
    private Float temperature;
    private Integer pulse;
    private String status;
}