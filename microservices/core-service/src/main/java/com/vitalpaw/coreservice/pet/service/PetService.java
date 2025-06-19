package com.vitalpaw.coreservice.pet.service;

import com.vitalpaw.coreservice.pet.dto.PetCreateDTO;
import com.vitalpaw.coreservice.pet.dto.PetDTO;
import com.vitalpaw.coreservice.pet.model.Pet;
import com.vitalpaw.coreservice.pet.repository.PetRepository;
import com.vitalpaw.coreservice.user.model.User;
import com.vitalpaw.coreservice.user.repository.UserRepository;
import com.vitalpaw.coreservice.breed.model.Breed;
import com.vitalpaw.coreservice.breed.repository.BreedRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class PetService {
    @Autowired
    private PetRepository petRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BreedRepository breedRepository;

    @Value("${app.image.storage-path}")
    private String uploadDir;

    @Transactional
    public PetDTO createPet(PetCreateDTO dto) {
        Pet pet = new Pet();
        pet.setName(dto.getName());
        pet.setSpecies(dto.getSpecies());
        if (dto.getBreedId() != null) {
            Breed breed = breedRepository.findById(dto.getBreedId())
                    .orElseThrow(() -> new IllegalArgumentException("Breed not found"));
            pet.setBreed(breed);
        }
        pet.setBirthDate(dto.getBirthDate());
        User owner = userRepository.findById(dto.getOwnerId())
                .orElseThrow(() -> new IllegalArgumentException("Owner not found"));
        pet.setOwner(owner);
        pet.setPhoto(dto.getPhoto());

        petRepository.save(pet);
        return mapToDTO(pet);
    }

    @Transactional(readOnly = true)
    public PetDTO getPetById(Long id) {
        Pet pet = petRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pet not found"));
        return mapToDTO(pet);
    }

    @Transactional
    public PetDTO updatePet(Long id, PetCreateDTO dto) {
        Pet pet = petRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pet not found"));
        
        if (dto.getName() != null) pet.setName(dto.getName());
        if (dto.getSpecies() != null) pet.setSpecies(dto.getSpecies());
        if (dto.getBreedId() != null) {
            Breed breed = breedRepository.findById(dto.getBreedId())
                    .orElseThrow(() -> new IllegalArgumentException("Breed not found"));
            pet.setBreed(breed);
        }
        if (dto.getBirthDate() != null) pet.setBirthDate(dto.getBirthDate());
        if (dto.getOwnerId() != null) {
            User owner = userRepository.findById(dto.getOwnerId())
                    .orElseThrow(() -> new IllegalArgumentException("Owner not found"));
            pet.setOwner(owner);
        }
        if (dto.getPhoto() != null) pet.setPhoto(dto.getPhoto());

        petRepository.save(pet);
        return mapToDTO(pet);
    }

    @Transactional
    public void deletePet(Long id) {
        if (!petRepository.existsById(id)) {
            throw new IllegalArgumentException("Pet not found");
        }
        petRepository.deleteById(id);
    }

    @Transactional
    public PetDTO uploadPetPhoto(Long id, MultipartFile file) throws IOException {
        Pet pet = petRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pet not found"));

        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        String contentType = file.getContentType();
        if (!contentType.equals("image/jpeg") && !contentType.equals("image/png")) {
            throw new IllegalArgumentException("Only JPEG and PNG images are supported");
        }

        // Create directory if it doesn't exist
        Path dirPath = Paths.get(uploadDir);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }

        // Generate unique filename: userId_petId_timestamp.extension
        String extension = contentType.equals("image/jpeg") ? ".jpg" : ".png";
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss"));
        String filename = String.format("user_%d_pet_%d_%s%s", pet.getOwner().getId(), id, timestamp, extension);
        Path filePath = dirPath.resolve(filename);

        // Save file
        Files.write(filePath, file.getBytes());

        // Update pet's photo path
        String relativePath = "/images/pets/" + filename;
        pet.setPhoto(relativePath);
        petRepository.save(pet);

        return mapToDTO(pet);
    }

    private PetDTO mapToDTO(Pet pet) {
        PetDTO dto = new PetDTO();
        dto.setId(pet.getId());
        dto.setName(pet.getName());
        dto.setSpecies(pet.getSpecies());
        dto.setBreedId(pet.getBreed() != null ? pet.getBreed().getId() : null);
        dto.setBirthDate(pet.getBirthDate());
        dto.setOwnerId(pet.getOwner().getId());
        dto.setPhoto(pet.getPhoto());
        return dto;
    }
}