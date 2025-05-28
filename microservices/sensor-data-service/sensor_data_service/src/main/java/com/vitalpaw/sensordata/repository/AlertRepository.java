package com.vitalpaw.sensordata.repository;

import com.vitalpaw.sensordata.model.Alert;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<Alert, Long> {
}