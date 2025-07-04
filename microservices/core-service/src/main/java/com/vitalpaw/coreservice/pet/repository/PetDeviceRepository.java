package com.vitalpaw.coreservice.pet.repository;

import com.vitalpaw.coreservice.pet.model.PetDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para gestionar operaciones de base de datos relacionadas con dispositivos de mascotas.
 */
@Repository
public interface PetDeviceRepository extends JpaRepository<PetDevice, Long> {
    Optional<PetDevice> findByDeviceId(String deviceId);
    
    /**
     * Elimina todos los dispositivos asociados a una mascota por su ID.
     * @param petId ID de la mascota
     */
    void deleteByPetId(Long petId);
}