package com.vitalpaw.coreservice.pet.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class PetDTO {
    private Long id;
    private String name;
    private String species;
    private Long breedId;
    private LocalDate birthDate;
    private Long ownerId;
    private String photo;
}