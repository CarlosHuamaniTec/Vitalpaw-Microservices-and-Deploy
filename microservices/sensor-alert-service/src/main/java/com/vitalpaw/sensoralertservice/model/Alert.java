package com.vitalpaw.sensoralert.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "alerts")
public class Alert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    @Column(name = "message", nullable = false)
    private String message;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "type", nullable = false, length = 50)
    private String type;

    @Column(name = "severity", nullable = false, length = 10)
    private String severity;

    @Column(name = "pulse")
    private Integer pulse;

    @Column(name = "temperature")
    private Float temperature;

    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }
}