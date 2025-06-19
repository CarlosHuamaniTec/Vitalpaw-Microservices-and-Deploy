package com.vitalpaw.coreservice.pet.repository;

import com.vitalpaw.coreservice.pet.model.PetDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PetDeviceRepository extends JpaRepository<PetDevice, Long> {
    Optional<PetDevice> findByDeviceId(String deviceId);
}