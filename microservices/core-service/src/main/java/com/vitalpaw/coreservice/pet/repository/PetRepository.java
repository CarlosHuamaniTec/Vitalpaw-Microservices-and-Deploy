package com.vitalpaw.coreservice.pet.repository;

import com.vitalpaw.coreservice.pet.model.Pet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PetRepository extends JpaRepository<Pet, Long> {
}