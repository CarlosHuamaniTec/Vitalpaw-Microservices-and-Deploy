package com.vitalpaw.sensoralertservice.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SensorDataResponseDTO {
    private String deviceId; // ID del dispositivo que envía los datos
    private Long petId;      // ID de la mascota asociada
    private float temperature;
    private int pulse;
    private String status;   // Estado de movimiento (ej. "Sin movimiento", "En movimiento", "Caído")
}