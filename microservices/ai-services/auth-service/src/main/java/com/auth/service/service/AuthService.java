package com.vitalpaw.auth.service;

import com.vitalpaw.auth.model.User;
import com.vitalpaw.auth.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private static final int MAX_USERS = 5;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean validateApiKey(String apiKey) {
        return userRepository.findByApiKey(apiKey) != null;
    }

    public User createUser(User user) {
        if (userRepository.count() >= MAX_USERS) {
            throw new RuntimeException("Maximum user limit reached (5)");
        }
        return userRepository.save(user);
    }

    public User getUser(String username) {
        return userRepository.findById(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User updateUser(String username, User user) {
        if (!userRepository.existsById(username)) {
            throw new RuntimeException("User not found");
        }
        user.setUsername(username);
        return userRepository.save(user);
    }

    public void deleteUser(String username) {
        if (!userRepository.existsById(username)) {
            throw new RuntimeException("User not found");
        }
        userRepository.deleteById(username);
    }
}