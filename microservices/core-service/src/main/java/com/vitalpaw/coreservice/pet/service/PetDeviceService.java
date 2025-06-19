package com.vitalpaw.coreservice.pet.service;

import com.vitalpaw.coreservice.pet.dto.PetDeviceDTO;
import com.vitalpaw.coreservice.pet.model.Pet;
import com.vitalpaw.coreservice.pet.model.PetDevice;
import com.vitalpaw.coreservice.pet.repository.PetDeviceRepository;
import com.vitalpaw.coreservice.pet.repository.PetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PetDeviceService {
    @Autowired
    private PetDeviceRepository petDeviceRepository;

    @Autowired
    private PetRepository petRepository;

    @Transactional
    public PetDeviceDTO createPetDevice(PetDeviceDTO dto) {
        Pet pet = petRepository.findById(dto.getPetId())
                .orElseThrow(() -> new IllegalArgumentException("Pet not found"));
        
        PetDevice petDevice = new PetDevice();
        petDevice.setPet(pet);
        petDevice.setDeviceId(dto.getDeviceId());
        petDevice.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);

        petDeviceRepository.save(petDevice);
        return mapToDTO(petDevice);
    }

    @Transactional(readOnly = true)
    public PetDeviceDTO getPetDeviceById(Long id) {
        PetDevice petDevice = petDeviceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pet device not found"));
        return mapToDTO(petDevice);
    }

    @Transactional(readOnly = true)
    public PetDeviceDTO getPetDeviceByDeviceId(String deviceId) {
        PetDevice petDevice = petDeviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("Pet device not found"));
        return mapToDTO(petDevice);
    }

    @Transactional
    public PetDeviceDTO updatePetDevice(Long id, PetDeviceDTO dto) {
        PetDevice petDevice = petDeviceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pet device not found"));
        
        if (dto.getPetId() != null) {
            Pet pet = petRepository.findById(dto.getPetId())
                    .orElseThrow(() -> new IllegalArgumentException("Pet not found"));
            petDevice.setPet(pet);
        }
        if (dto.getDeviceId() != null) {
            petDevice.setDeviceId(dto.getDeviceId());
        }
        if (dto.getIsActive() != null) {
            petDevice.setIsActive(dto.getIsActive());
        }

        petDeviceRepository.save(petDevice);
        return mapToDTO(petDevice);
    }

    @Transactional
    public void deletePetDevice(Long id) {
        if (!petDeviceRepository.existsById(id)) {
            throw new IllegalArgumentException("Pet device not found");
        }
        petDeviceRepository.deleteById(id);
    }

    private PetDeviceDTO mapToDTO(PetDevice petDevice) {
        PetDeviceDTO dto = new PetDeviceDTO();
        dto.setId(petDevice.getId());
        dto.setPetId(petDevice.getPet().getId());
        dto.setDeviceId(petDevice.getDeviceId());
        dto.setIsActive(petDevice.getIsActive());
        return dto;
    }
}