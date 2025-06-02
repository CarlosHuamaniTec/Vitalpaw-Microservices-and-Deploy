package com.vitalpaw.userservice.controller;

import com.vitalpaw.userservice.dto.UserDTO;
import com.vitalpaw.userservice.model.Pet;
import com.vitalpaw.userservice.model.User;
import com.vitalpaw.userservice.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<User> createUser(@Valid @RequestBody UserDTO userDTO) {
        User user = userService.createUser(userDTO);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/login")
    public ResponseEntity<Void> loginUser(@RequestBody UserDTO userDTO) {
        String token = userService.loginUser(userDTO);
        if (token == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok().header("Authorization", "Bearer " + token).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @Valid @RequestBody UserDTO userDTO) {
        User user = userService.updateUser(id, userDTO);
        if (user == null) {
            return ResponseEntity.status(404).build();
        }
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
        return ResponseEntity.ok(users != null ? users : List.of());
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        Optional<User> user = userService.getUserById(id);
        return user.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{userId}/pets")
    public ResponseEntity<List<Pet>> getPetsByUserId(@PathVariable Long userId) {
        List<Pet> pets = userService.getPetsByUserId(userId);
        return ResponseEntity.ok(pets != null ? pets : List.of());
    }

    @PostMapping("/{userId}/vet-ai")
    public ResponseEntity<Void> prepareVetAiData(@PathVariable Long userId) {
        userService.prepareVetAiDataForUser(userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody UserDTO userDTO) {
        User user = userService.findByEmail(userDTO.getEmail());
        if (user == null) {
            return ResponseEntity.status(404).body("Correo no encontrado.");
        }
        userService.sendResetPasswordEmail(user);
        return ResponseEntity.ok("Correo de recuperación enviado.");
    }

    @GetMapping("/verify")
    public String verifyEmail(@RequestParam String token, Model model) {
        User user = userService.findByVerificationToken(token);
        if (user != null && !user.isVerified()) {
            user.setVerified(true);
            user.setVerificationToken(null);
            userService.save(user);
            model.addAttribute("success", "Felicidades, ya estás registrado. Puedes <a href='/users/login'>iniciar sesión</a>.");
            return "verify";
        }
        model.addAttribute("error", "Token inválido o cuenta ya verificada.");
        return "verify";
    }

    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam String token, Model model) {
        User user = userService.findByVerificationToken(token);
        if (user != null) {
            model.addAttribute("token", token);
            return "reset-password";
        }
        model.addAttribute("error", "Token inválido.");
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String token, @RequestParam String password, Model model) {
        User user = userService.findByVerificationToken(token);
        if (user != null) {
            user.setPassword(password);
            user.setVerificationToken(null);
            userService.save(user);
            model.addAttribute("success", "Contraseña restablecida exitosamente. <a href='/users/login'>Iniciar sesión</a>.");
            return "verify";
        }
        model.addAttribute("error", "Token inválido.");
        return "reset-password";
    }
}