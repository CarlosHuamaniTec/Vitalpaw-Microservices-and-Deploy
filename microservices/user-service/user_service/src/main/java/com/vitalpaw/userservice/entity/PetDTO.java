package com.vitalpaw.userservice.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class PetDTO {
    private String name;
    private String species;
    private String breed;
    private String age;
    private LocalDate birthDate;
}