package com.vitalpaw.userservice.service;

import com.vitalpaw.userservice.model.User;
import com.vitalpaw.userservice.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.jwtService = jwtService;
    }

    public User registerUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setVerificationToken(UUID.randomUUID().toString());
        user.setVerified(false);
        return userRepository.save(user);
    }

    public String loginUser(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent() && passwordEncoder.matches(password, userOpt.get().getPassword())) {
            return jwtService.generateToken(username);
        }
        throw new RuntimeException("Invalid credentials");
    }

    public User verifyUser(String token) {
        Optional<User> userOpt = userRepository.findByVerificationToken(token);
        userOpt.ifPresent(user -> {
            user.setVerified(true);
            user.setVerificationToken(null);
            userRepository.save(user);
        });
        return userOpt.orElseThrow(() -> new RuntimeException("Invalid verification token"));
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}