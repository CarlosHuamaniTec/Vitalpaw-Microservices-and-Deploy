package com.vitalpaw.sensordata.service;

import com.vitalpaw.sensordata.model.Alert;
import com.vitalpaw.sensordata.repository.AlertRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AlertService {
    private final AlertRepository alertRepository;

    public AlertService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    public void saveAlertFromFirebase(String message, String severity) {
        Alert alert = new Alert();
        alert.setMessage(message);
        alert.setTimestamp(LocalDateTime.now());
        alert.setSeverity(severity);
        alertRepository.save(alert);
    }
}