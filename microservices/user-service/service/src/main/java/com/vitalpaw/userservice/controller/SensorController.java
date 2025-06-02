package com.vitalpaw.userservice.controller;

import com.vitalpaw.userservice.model.Sensor;
import com.vitalpaw.userservice.service.SensorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users/sensors")
public class SensorController {

    private final SensorService sensorService;

    @Autowired
    public SensorController(SensorService sensorService) {
        this.sensorService = sensorService;
    }

    @PostMapping
    public ResponseEntity<?> createSensor(@RequestBody Sensor sensor) {
        Sensor created = sensorService.createSensor(sensor);
        return ResponseEntity.ok("Sensor created successfully: " + created.getSensorId());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getSensorById(@PathVariable Long id) {
        return sensorService.getSensorById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/sensorId/{sensorId}")
    public ResponseEntity<?> getSensorBySensorId(@PathVariable String sensorId) {
        return sensorService.getSensorBySensorId(sensorId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateSensor(@PathVariable Long id, @RequestBody Sensor sensor) {
        Sensor updated = sensorService.updateSensor(id, sensor);
        return ResponseEntity.ok("Sensor updated successfully: " + updated.getSensorId());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSensor(@PathVariable Long id) {
        sensorService.deleteSensor(id);
        return ResponseEntity.ok("Sensor deleted successfully");
    }
}