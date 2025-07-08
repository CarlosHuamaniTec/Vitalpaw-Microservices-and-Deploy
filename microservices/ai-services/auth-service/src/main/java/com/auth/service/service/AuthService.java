package com.auth.service.service;

import com.auth.service.model.Auth;
import com.auth.service.repository.AuthRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class AuthService {
    private final AuthRepository authRepository;
    private int maxApiKeys = 5;

    public AuthService(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    public boolean validateApiKey(String apiKey) {
        return authRepository.findByApiKey(apiKey).isPresent();
    }

    public Auth createApiKey(Auth auth) {
        if (authRepository.count() >= maxApiKeys) {
            throw new RuntimeException("Maximum API key limit reached (" + maxApiKeys + ")");
        }
        if (authRepository.findByApiKey(auth.getApiKey()).isPresent()) {
            throw new RuntimeException("API key already exists");
        }
        return authRepository.save(auth);
    }

    public List<Auth> getAllApiKeys() {
        return authRepository.findAll();
    }

    public long countApiKeys() {
        return authRepository.count();
    }

    public Auth getApiKey(String apiKey) {
        return authRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new RuntimeException("API key not found"));
    }

    public void deleteApiKey(String apiKey) {
        Optional<Auth> authToDelete = authRepository.findByApiKey(apiKey);
        if (authToDelete.isPresent()) {
            authRepository.delete(authToDelete.get());
        } else {
            throw new RuntimeException("API key not found");
        }
    }

    public int getMaxApiKeys() {
        return maxApiKeys;
    }

    public void setMaxApiKeys(int maxApiKeys) {
        if (maxApiKeys < 1) {
            throw new RuntimeException("Maximum API keys must be at least 1");
        }
        this.maxApiKeys = maxApiKeys;
    }
}