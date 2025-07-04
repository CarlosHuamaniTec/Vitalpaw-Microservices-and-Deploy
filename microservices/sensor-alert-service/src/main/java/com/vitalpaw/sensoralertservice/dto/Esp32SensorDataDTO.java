package com.vitalpaw.sensoralertservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor; // Asegurarse de tener el constructor sin argumentos
import lombok.AllArgsConstructor; // Opcional, pero útil para crear instancias de prueba

@Data
@NoArgsConstructor // Necesario para que Jackson pueda deserializar
@AllArgsConstructor // Útil para crear instancias fácilmente
public class Esp32SensorDataDTO {
    @JsonProperty("ecg_raw")
    private int ecg_raw;

    @JsonProperty("temperatura_celsius")
    private float temperatura_celsius;

    @JsonProperty("movimiento")
    private String movimiento;
}