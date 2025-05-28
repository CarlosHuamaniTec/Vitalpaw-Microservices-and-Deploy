package com.vitalpaw.mainclient.model;

import lombok.Data;

@Data
public class BiometricData {
    private String petId;
    private int heartRate;     // Frecuencia cardíaca (lpm)
    private double temperature; // Temperatura (°C)
    private double x;          // Acelerómetro X (m/s²)
    private double y;          // Acelerómetro Y (m/s²)
    private double z;          // Acelerómetro Z (m/s²)
}