package com.vitalpaw.auth.controller;

import com.vitalpaw.auth.dto.UserDTO;
import com.vitalpaw.auth.model.User;
import com.vitalpaw.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Validate API Key")
    @GetMapping("/validate")
    public ResponseEntity<?> validate(@RequestHeader("X-API-Key") String apiKey) {
        if (authService.validateApiKey(apiKey)) {
            return ResponseEntity.ok().body("Valid API Key");
        }
        return ResponseEntity.status(401).body("Invalid API Key");
    }

    @Operation(summary = "Health Check")
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok().body("Auth Service is running");
    }

    @Operation(summary = "Create User (localhost only)")
    @PostMapping("/users")
    public ResponseEntity<UserDTO> createUser(@RequestBody UserDTO userDTO, @RequestHeader("Host") String host) {
        if (!host.contains("localhost") && !host.contains("127.0.0.1")) {
            return ResponseEntity.status(403).body(null);
        }
        User user = new User();
        user.setUsername(userDTO.getUsername());
        user.setApiKey(userDTO.getApiKey());
        user = authService.createUser(user);
        userDTO.setUsername(user.getUsername());
        userDTO.setApiKey(user.getApiKey());
        return ResponseEntity.ok(userDTO);
    }

    @Operation(summary = "Get User (localhost only)")
    @GetMapping("/users/{username}")
    public ResponseEntity<UserDTO> getUser(@PathVariable String username, @RequestHeader("Host") String host) {
        if (!host.contains("localhost") && !host.contains("127.0.0.1")) {
            return ResponseEntity.status(403).body(null);
        }
        User user = authService.getUser(username);
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername(user.getUsername());
        userDTO.setApiKey(user.getApiKey());
        return ResponseEntity.ok(userDTO);
    }

    @Operation(summary = "Update User (localhost only)")
    @PutMapping("/users/{username}")
    public ResponseEntity<UserDTO> updateUser(@PathVariable String username, @RequestBody UserDTO userDTO, @RequestHeader("Host") String host) {
        if (!host.contains("localhost") && !host.contains("127.0.0.1")) {
            return ResponseEntity.status(403).body(null);
        }
        User user = new User();
        user.setApiKey(userDTO.getApiKey());
        user = authService.updateUser(username, user);
        userDTO.setUsername(user.getUsername());
        userDTO.setApiKey(user.getApiKey());
        return ResponseEntity.ok(userDTO);
    }

    @Operation(summary = "Delete User (localhost only)")
    @DeleteMapping("/users/{username}")
    public ResponseEntity<Void> deleteUser(@PathVariable String username, @RequestHeader("Host") String host) {
        if (!host.contains("localhost") && !host.contains("127.0.0.1")) {
            return ResponseEntity.status(403).body(null);
        }
        authService.deleteUser(username);
        return ResponseEntity.ok().build();
    }
}