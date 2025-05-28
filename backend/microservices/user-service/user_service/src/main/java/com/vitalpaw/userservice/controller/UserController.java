package com.vitalpaw.userservice.controller;

import com.vitalpaw.userservice.model.Pet;
import com.vitalpaw.userservice.model.User;
import com.vitalpaw.userservice.service.PetService;
import com.vitalpaw.userservice.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;
    private final PetService petService;

    public UserController(UserService userService, PetService petService) {
        this.userService = userService;
        this.petService = petService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody User user) {
        userService.registerUser(user);
        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody User user) {
        String token = userService.loginUser(user.getUsername(), user.getPassword());
        return ResponseEntity.ok(token);
    }

    @GetMapping("/all")
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    // Endpoints para gestionar mascotas
    @PostMapping("/{userId}/pets")
    public ResponseEntity<Pet> createPet(@PathVariable Long userId, @RequestBody Pet pet) {
        User user = new User(); // Simulación: obtén el usuario real desde el token
        user.setId(userId);
        pet.setOwner(user);
        Pet savedPet = petService.createPet(pet);
        return ResponseEntity.ok(savedPet);
    }

    @GetMapping("/{userId}/pets")
    public ResponseEntity<List<Pet>> getPetsByOwnerId(@PathVariable Long userId) {
        List<Pet> pets = petService.getPetsByOwnerId(userId);
        return ResponseEntity.ok(pets);
    }

    @GetMapping("/{userId}/pets/{petId}")
    public ResponseEntity<Pet> getPetById(@PathVariable Long userId, @PathVariable Long petId) {
        Pet pet = petService.getPetById(petId)
                .orElseThrow(() -> new RuntimeException("Pet not found"));
        return ResponseEntity.ok(pet);
    }

    @PutMapping("/{userId}/pets/{petId}")
    public ResponseEntity<Pet> updatePet(@PathVariable Long userId, @PathVariable Long petId, @RequestBody Pet petDetails) {
        Pet updatedPet = petService.updatePet(petId, petDetails);
        return ResponseEntity.ok(updatedPet);
    }

    @DeleteMapping("/{userId}/pets/{petId}")
    public ResponseEntity<Void> deletePet(@PathVariable Long userId, @PathVariable Long petId) {
        petService.deletePet(petId);
        return ResponseEntity.ok().build();
    }
}