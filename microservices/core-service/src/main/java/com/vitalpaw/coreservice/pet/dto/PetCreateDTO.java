package com.vitalpaw.coreservice.pet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

@Data
@Schema(description = "Datos necesarios para crear una mascota")
public class PetCreateDTO {

    @NotBlank
    @Size(max = 100)
    @Schema(example = "Max", description = "Nombre de la mascota")
    private String name;

    @NotBlank
    @Size(max = 100)
    @Schema(example = "Perro", description = "Especie de la mascota")
    private String species;

    @Schema(example = "1", description = "ID de la raza asociada (opcional)")
    private Long breedId;

    @Schema(example = "2020-05-15", description = "Fecha de nacimiento de la mascota")
    private LocalDate birthDate;

    @NotNull
    @Schema(example = "1", description = "ID del dueño de la mascota")
    private Long ownerId;

    @Size(max = 255)
    @Schema(example = "https://example.com/photo.jpg",  description = "URL de foto de la mascota (opcional)")
    private String photo;
}