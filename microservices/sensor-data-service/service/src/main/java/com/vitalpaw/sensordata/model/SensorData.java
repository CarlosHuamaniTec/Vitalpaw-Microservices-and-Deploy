package com.vitalpaw.sensordataservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class SensorData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long petId; // Vincula a una mascota
    private double temperature;
    private int heartRate;
    private double x;
    private double y;
    private double z;
    private String breed;
    private LocalDateTime timestamp;
}