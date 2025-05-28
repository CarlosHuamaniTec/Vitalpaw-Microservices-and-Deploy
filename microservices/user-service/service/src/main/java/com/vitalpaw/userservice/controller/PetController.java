package com.vitalpaw.userservice.controller;

import com.vitalpaw.userservice.dto.PetDTO;
import com.vitalpaw.userservice.model.Pet;
import com.vitalpaw.userservice.model.User;
import com.vitalpaw.userservice.service.PetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/users/pets")
public class PetController {

    private final PetService petService;

    @Autowired
    public PetController(PetService petService) {
        this.petService = petService;
    }

    @PostMapping("/{userId}")
    public ResponseEntity<?> createPet(@PathVariable Long userId, @RequestBody PetDTO petDTO) {
        Pet pet = new Pet();
        pet.setName(petDTO.getName());
        pet.setSpecies(petDTO.getSpecies());
        pet.setBreed(petDTO.getBreed());
        pet.setBirthDate(petDTO.getBirthDate());
        // Asignar owner (simulado, en producción verifica usuario)
        User owner = new User();
        owner.setId(userId);
        pet.setOwner(owner);
        petService.createPet(pet);
        return ResponseEntity.ok("Pet created successfully");
    }

    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<List<Pet>> getPetsByOwnerId(@PathVariable Long ownerId) {
        List<Pet> pets = petService.getPetsByOwnerId(ownerId);
        return ResponseEntity.ok(pets);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getPetById(@PathVariable Long id) {
        Optional<Pet> pet = petService.getPetById(id);
        return pet.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updatePet(@PathVariable Long id, @RequestBody PetDTO petDTO) {
        Pet petDetails = new Pet();
        petDetails.setName(petDTO.getName());
        petDetails.setSpecies(petDTO.getSpecies());
        petDetails.setBreed(petDTO.getBreed());
        petDetails.setBirthDate(petDTO.getBirthDate());
        Pet updatedPet = petService.updatePet(id, petDetails);
        return ResponseEntity.ok("Pet updated successfully: " + updatedPet.getName());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePet(@PathVariable Long id) {
        petService.deletePet(id);
        return ResponseEntity.ok("Pet deleted successfully");
    }

    @GetMapping("/{petId}/owner/token")
    public ResponseEntity<String> getOwnerToken(@PathVariable Long petId) {
        Pet pet = petService.getPetById(petId)
                .orElseThrow(() -> new RuntimeException("Pet not found"));
        // Simulación: en producción, obtén el token del User
        return ResponseEntity.ok("dummy-token-from-user"); // Reemplazar con lógica real
    }
}