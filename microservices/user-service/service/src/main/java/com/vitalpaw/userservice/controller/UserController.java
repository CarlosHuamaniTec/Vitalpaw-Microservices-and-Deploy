package com.vitalpaw.userservice.controller;

import com.vitalpaw.userservice.dto.UserDTO;
import com.vitalpaw.userservice.model.Pet;
import com.vitalpaw.userservice.model.User;
import com.vitalpaw.userservice.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<User> createUser(@RequestBody UserDTO userDTO) {
        User user = userService.createUser(userDTO);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/login")
    public ResponseEntity<User> loginUser(@RequestBody UserDTO userDTO) {
        User user = userService.loginUser(userDTO);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody UserDTO userDTO) {
        User user = userService.updateUser(id, userDTO);
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        Optional<User> user = userService.getUserById(id);
        return user.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{userId}/pets")
    public ResponseEntity<List<Pet>> getPetsByUserId(@PathVariable Long userId) {
        List<Pet> pets = userService.getPetsByUserId(userId);
        return ResponseEntity.ok(pets);
    }

    @PostMapping("/{userId}/vet-ai")
    public ResponseEntity<Void> prepareVetAiData(@PathVariable Long userId) {
        userService.prepareVetAiDataForUser(userId);
        return ResponseEntity.ok().build();
    }
}