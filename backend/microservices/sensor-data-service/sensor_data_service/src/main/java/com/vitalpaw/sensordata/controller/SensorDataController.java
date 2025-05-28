package com.vitalpaw.sensordata.controller;

import com.vitalpaw.sensordata.service.AlertService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/sensor-data")
public class SensorDataController {
    private final AlertService alertService;

    public SensorDataController(AlertService alertService) {
        this.alertService = alertService;
    }

    @PostMapping("/alerts")
    public ResponseEntity<String> saveAlert(@RequestBody Map<String, String> alertData) {
        String message = alertData.get("message");
        String severity = alertData.get("severity");
        alertService.saveAlertFromFirebase(message, severity);
        return ResponseEntity.ok("Alert saved successfully");
    }
}