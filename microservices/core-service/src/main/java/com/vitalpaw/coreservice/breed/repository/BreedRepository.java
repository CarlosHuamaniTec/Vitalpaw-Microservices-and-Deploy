
package com.vitalpaw.coreservice.breed.repository;

import com.vitalpaw.coreservice.breed.model.Breed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BreedRepository extends JpaRepository<Breed, Long> {
}