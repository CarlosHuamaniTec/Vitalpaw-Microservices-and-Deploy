package com.auth.service.controller;

import com.auth.service.model.Auth;
import com.auth.service.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validate(@RequestHeader("X-API-Key") String apiKey) {
        if (authService.validateApiKey(apiKey)) {
            return ResponseEntity.ok().body("Valid API Key");
        }
        return ResponseEntity.status(401).body("Invalid API Key");
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok().body("Auth Service is running");
    }

    @PostMapping("/api-keys")
    public ResponseEntity<Auth> createApiKey(@RequestBody Auth auth) {
        auth = authService.createApiKey(auth);
        return ResponseEntity.ok(auth);
    }

    @GetMapping("/api-keys/{apiKey}")
    public ResponseEntity<Auth> getApiKey(@PathVariable String apiKey) {
        Auth auth = authService.getApiKey(apiKey);
        return ResponseEntity.ok(auth);
    }

    @DeleteMapping("/api-keys/{apiKey}")
    public ResponseEntity<Void> deleteApiKey(@PathVariable String apiKey) {
        authService.deleteApiKey(apiKey);
        return ResponseEntity.ok().build();
    }
}