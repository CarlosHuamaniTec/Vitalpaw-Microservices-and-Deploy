package com.vitalpaw.coreservice.alert.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Schema(description = "Datos de una alerta registrada para una mascota")
public class AlertDTO {

    @Schema(example = "1", description = "ID único de la alerta")
    private Long id;

    @Schema(example = "1", description = "ID de la mascota asociada")
    private Long petId;

    @Schema(example = "La temperatura está fuera de rango normal.", description = "Mensaje descriptivo de la alerta")
    private String message;

    @Schema(example = "2025-04-05T10:00:00", description = "Fecha y hora en que se generó la alerta")
    private LocalDateTime timestamp;

    @Schema(example = "temperature_alert", description = "Tipo de alerta (ej: temperatura, pulso, etc.)")
    private String type;

    @Schema(example = "high", description = "Nivel de gravedad: high/medium/low")
    private String severity;

    @Schema(example = "140", description = "Valor del pulso registrado (opcional)")
    private Integer pulse;

    @Schema(example = "39.5", description = "Temperatura corporal registrada (opcional)")
    private Float temperature;
}