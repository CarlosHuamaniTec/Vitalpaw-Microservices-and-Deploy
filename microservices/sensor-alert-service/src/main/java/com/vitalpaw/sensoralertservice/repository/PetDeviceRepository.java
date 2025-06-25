package com.vitalpaw.sensoralertservice.repository;

import com.vitalpaw.sensoralertservice.entity.PetDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PetDeviceRepository extends JpaRepository<PetDevice, Long> {
    Optional<PetDevice> findByDeviceId(String deviceId);
}