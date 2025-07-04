package com.vitalpaw.coreservice.pet.service;

import com.vitalpaw.coreservice.pet.dto.PetCreateDTO;
import com.vitalpaw.coreservice.pet.dto.PetDTO;
import com.vitalpaw.coreservice.pet.model.Pet;
import com.vitalpaw.coreservice.pet.repository.PetRepository;
import com.vitalpaw.coreservice.pet.repository.PetDeviceRepository;
import com.vitalpaw.coreservice.alert.repository.AlertRepository;
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
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestionar la lógica de negocio relacionada con mascotas.
 */
@Service
public class PetService {
    @Autowired
    private PetRepository petRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BreedRepository breedRepository;

    @Autowired
    private PetDeviceRepository petDeviceRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Value("${app.image.storage-path}")
    private String uploadDir;

    @Transactional
    public PetDTO createPet(PetCreateDTO dto) {
        Pet pet = new Pet();
        pet.setName(dto.getName());
        pet.setSpecies(dto.getSpecies());
        if (dto.getBreedId() != null) {
            Breed breed = breedRepository.findById(dto.getBreedId())
                    .orElseThrow(() -> new IllegalArgumentException("Raza no encontrada"));
            pet.setBreed(breed);
        }
        pet.setBirthDate(dto.getBirthDate());
        User owner = userRepository.findById(dto.getOwnerId())
                .orElseThrow(() -> new IllegalArgumentException("Dueño no encontrado"));
        pet.setOwner(owner);
        pet.setPhoto(dto.getPhoto());

        petRepository.save(pet);
        return mapToDTO(pet);
    }

    @Transactional(readOnly = true)
    public PetDTO getPetById(Long id) {
        Pet pet = petRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mascota no encontrada"));
        return mapToDTO(pet);
    }

    @Transactional(readOnly = true)
    public List<PetDTO> getPetsByOwnerId(Long ownerId) {
        userRepository.findById(ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Dueño no encontrado"));
        List<Pet> pets = petRepository.findByOwnerId(ownerId);
        return pets.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Transactional
    public PetDTO updatePet(Long id, PetCreateDTO dto) {
        Pet pet = petRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mascota no encontrada"));
        
        if (dto.getName() != null) pet.setName(dto.getName());
        if (dto.getSpecies() != null) pet.setSpecies(dto.getSpecies());
        if (dto.getBreedId() != null) {
            Breed breed = breedRepository.findById(dto.getBreedId())
                    .orElseThrow(() -> new IllegalArgumentException("Raza no encontrada"));
            pet.setBreed(breed);
        }
        if (dto.getBirthDate() != null) pet.setBirthDate(dto.getBirthDate());
        if (dto.getOwnerId() != null) {
            User owner = userRepository.findById(dto.getOwnerId())
                    .orElseThrow(() -> new IllegalArgumentException("Dueño no encontrado"));
            pet.setOwner(owner);
        }
        if (dto.getPhoto() != null) pet.setPhoto(dto.getPhoto());

        petRepository.save(pet);
        return mapToDTO(pet);
    }

    @Transactional
    public void deletePet(Long id) {
        Pet pet = petRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mascota no encontrada"));
        
        // Eliminar dispositivos asociados
        petDeviceRepository.deleteByPetId(id);
        
        // Eliminar alertas asociadas
        alertRepository.deleteByPetId(id);
        
        // Eliminar la mascota
        petRepository.deleteById(id);
    }

    @Transactional
    public PetDTO uploadPetPhoto(Long id, MultipartFile file) throws IOException {
        Pet pet = petRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mascota no encontrada"));

        if (file.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío");
        }
        String contentType = file.getContentType();
        if (!contentType.equals("image/jpeg") && !contentType.equals("image/png")) {
            throw new IllegalArgumentException("Solo se admiten imágenes JPEG y PNG");
        }

        Path dirPath = Paths.get(uploadDir);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }

        String extension = contentType.equals("image/jpeg") ? ".jpg" : ".png";
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss"));
        String filename = String.format("user_%d_pet_%d_%s%s", pet.getOwner().getId(), id, timestamp, extension);
        Path filePath = dirPath.resolve(filename);

        Files.write(filePath, file.getBytes());

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