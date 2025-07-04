package com.vitalpaw.coreservice.alert.repository;

import com.vitalpaw.coreservice.alert.model.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para gestionar operaciones de base de datos relacionadas con alertas.
 */
@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findByPetIdOrderByTimestampDesc(Long petId);

    @Query("SELECT a FROM Alert a WHERE a.pet.id = :petId ORDER BY a.timestamp DESC")
    List<Alert> findTopByPetId(@Param("petId") Long petId, @Param("org.springframework.data.domain.Pageable") org.springframework.data.domain.Pageable pageable);

    /**
     * Elimina todas las alertas asociadas a una mascota por su ID.
     * @param petId ID de la mascota
     */
    void deleteByPetId(Long petId);
}