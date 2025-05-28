package com.vitalpaw.sensordataservice.repository;

import com.vitalpaw.sensordataservice.model.SensorData;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SensorDataRepository extends JpaRepository<SensorData, Long> {
}