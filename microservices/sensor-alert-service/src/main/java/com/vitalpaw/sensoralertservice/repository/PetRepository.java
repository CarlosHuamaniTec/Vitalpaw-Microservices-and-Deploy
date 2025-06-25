package com.vitalpaw.sensoralertservice.repository;

import com.vitalpaw.sensoralertservice.entity.Pet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PetRepository extends JpaRepository<Pet, Long> {
}