package com.vitalpaw.sensoralertservice.repository;

import com.vitalpaw.sensoralertservice.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {
}