package com.vitalpaw.coreservice.breed.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "breeds")
public class Breed {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 200, unique = true)
    private String name;

    @Column(name = "species", nullable = false, length = 100)
    private String species;

    @Column(name = "max_temperature", nullable = false)
    private Float maxTemperature;

    @Column(name = "min_temperature", nullable = false)
    private Float minTemperature;

    @Column(name = "max_heart_rate", nullable = false)
    private Integer maxHeartRate;

    @Column(name = "min_heart_rate", nullable = false)
    private Integer minHeartRate;
}