package com.vitalpaw.coreservice.pet.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

@Data
public class PetDeviceDTO {
    private Long id;

    @NotNull
    private Long petId;

    @NotBlank
    @Size(max = 50)
    private String deviceId;

    private Boolean isActive;
}