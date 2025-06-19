package com.vitalpaw.coreservice.breed.service;

import com.vitalpaw.coreservice.breed.dto.BreedDTO;
import com.vitalpaw.coreservice.breed.model.Breed;
import com.vitalpaw.coreservice.breed.repository.BreedRepository;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BreedService {
    @Autowired
    private BreedRepository breedRepository;

    @Transactional
    public BreedDTO createBreed(BreedDTO dto) {
        Breed breed = new Breed();
        breed.setName(dto.getName());
        breed.setSpecies(dto.getSpecies());
        breed.setMaxTemperature(dto.getMaxTemperature());
        breed.setMinTemperature(dto.getMinTemperature());
        breed.setMaxHeartRate(dto.getMaxHeartRate());
        breed.setMinHeartRate(dto.getMinHeartRate());

        breedRepository.save(breed);
        return mapToDTO(breed);
    }

    @Transactional(readOnly = true)
    public BreedDTO getBreedById(Long id) {
        Breed breed = breedRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Breed not found"));
        return mapToDTO(breed);
    }

    @Transactional(readOnly = true)
    public List<BreedDTO> getAllBreeds() {
        List<Breed> breeds = breedRepository.findAll();
        return breeds.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    private BreedDTO mapToDTO(Breed breed) {
        BreedDTO dto = new BreedDTO();
        dto.setId(breed.getId());
        dto.setName(breed.getName());
        dto.setSpecies(breed.getSpecies());
        dto.setMaxTemperature(breed.getMaxTemperature());
        dto.setMinTemperature(breed.getMinTemperature());
        dto.setMaxHeartRate(breed.getMaxHeartRate());
        dto.setMinHeartRate(breed.getMinHeartRate());
        return dto;
    }
}