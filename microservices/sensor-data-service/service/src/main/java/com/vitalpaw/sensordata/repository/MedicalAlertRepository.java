package com.vitalpaw.sensordataservice.repository;

import com.vitalpaw.sensordataservice.model.MedicalAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface MedicalAlertRepository extends JpaRepository<MedicalAlert, Long> {
    List<MedicalAlert> findByPetIdAndTimestampAfter(Long petId, LocalDateTime timestamp);
}