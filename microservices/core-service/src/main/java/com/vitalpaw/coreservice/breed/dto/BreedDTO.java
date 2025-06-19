package com.vitalpaw.coreservice.breed.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@Schema(description = "Datos públicos de una raza de mascota")
public class BreedDTO {

    @Schema(example = "1", description = "ID único de la raza")
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 200, message = "El nombre no puede exceder los 200 caracteres")
    @Schema(example = "Labrador Retriever", description = "Nombre de la raza")
    private String name;

    @NotBlank(message = "La especie es obligatoria")
    @Size(max = 100, message = "La especie no puede exceder los 100 caracteres")
    @Schema(example = "Perro", description = "Especie a la que pertenece la raza")
    private String species;

    @NotNull(message = "La temperatura máxima es obligatoria")
    @Positive(message = "Debe ser un número positivo")
    @Schema(example = "39.5", description = "Temperatura corporal máxima normal para esta raza")
    private Float maxTemperature;

    @NotNull(message = "La temperatura mínima es obligatoria")
    @Positive(message = "Debe ser un número positivo")
    @Schema(example = "37.5", description = "Temperatura corporal mínima normal para esta raza")
    private Float minTemperature;

    @NotNull(message = "El pulso máximo es obligatorio")
    @Positive(message = "Debe ser un número positivo")
    @Schema(example = "120", description = "Pulso cardíaco máximo normal para esta raza")
    private Integer maxHeartRate;

    @NotNull(message = "El pulso mínimo es obligatorio")
    @Positive(message = "Debe ser un número positivo")
    @Schema(example = "60", description = "Pulso cardíaco mínimo normal para esta raza")
    private Integer minHeartRate;
}