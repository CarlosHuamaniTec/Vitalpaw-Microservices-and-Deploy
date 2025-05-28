package com.vitalpaw.userservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

@Entity
@Table(name = "pets")
@Getter
@Setter
public class Pet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String species;

    @Column
    private String breed;

    @Column(nullable = false)
    private LocalDate birthDate;

    @Column
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Transient
    private String age;

    // Método para calcular la edad automáticamente
    @PrePersist
    @PreUpdate
    public void calculateAge() {
        if (birthDate != null) {
            LocalDate currentDate = LocalDate.now(); // 2025-05-28
            Period period = Period.between(birthDate, currentDate);
            int years = period.getYears();
            int months = period.getMonths();
            this.age = String.format("%d años y %d meses", years, months);
        } else {
            this.age = "Fecha de nacimiento no disponible";
        }
    }
}