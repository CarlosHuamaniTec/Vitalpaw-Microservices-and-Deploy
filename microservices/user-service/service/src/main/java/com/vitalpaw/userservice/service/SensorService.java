package com.vitalpaw.userservice.service;

import com.vitalpaw.userservice.model.Sensor;
import com.vitalpaw.userservice.repository.SensorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SensorService {

    @Autowired
    private SensorRepository sensorRepository;

    public Sensor createSensor(Sensor sensor) {
        return sensorRepository.save(sensor);
    }

    public Optional<Sensor> getSensorById(Long id) {
        return sensorRepository.findById(id);
    }

    public Optional<Sensor> getSensorBySensorId(String sensorId) {
        return sensorRepository.findBySensorId(sensorId);
    }

    public Sensor updateSensor(Long id, Sensor sensorDetails) {
        Sensor sensor = sensorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sensor not found"));
        sensor.setSensorId(sensorDetails.getSensorId());
        sensor.setPetId(sensorDetails.getPetId());
        return sensorRepository.save(sensor);
    }

    public void deleteSensor(Long id) {
        sensorRepository.deleteById(id);
    }
}