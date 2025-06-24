package com.vitalpaw.coreservice.user.service;

import com.vitalpaw.coreservice.user.dto.UserCreateDTO;
import com.vitalpaw.coreservice.user.dto.UserDTO;
import com.vitalpaw.coreservice.user.model.User;
import com.vitalpaw.coreservice.user.model.UserConfirmationToken;
import com.vitalpaw.coreservice.user.model.PasswordResetToken;
import com.vitalpaw.coreservice.user.repository.UserRepository;
import com.vitalpaw.coreservice.user.repository.UserConfirmationTokenRepository;
import com.vitalpaw.coreservice.user.repository.PasswordResetTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.mail.MessagingException;
import java.time.LocalDateTime;
import java.util.Random;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserConfirmationTokenRepository tokenRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    private static final Random RANDOM = new Random();

    private String generateFourDigitToken() {
        return String.format("%04d", RANDOM.nextInt(10000)); // Genera un número de 4 dígitos (0000-9999)
    }

    @Transactional
    public UserDTO createUser(UserCreateDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        User user = new User();
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setPhone(dto.getPhone());
        user.setCity(dto.getCity());
        user.setUsername(dto.getUsername());
        user.setIsConfirmed(false);

        userRepository.save(user);

        UserConfirmationToken token = new UserConfirmationToken();
        token.setUser(user);
        token.setToken(generateFourDigitToken());
        token.setExpiresAt(LocalDateTime.now().plusHours(24));
        tokenRepository.save(token);

        try {
            emailService.sendConfirmationEmail(user.getEmail(), token.getToken());
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send confirmation email", e);
        }

        return mapToDTO(user);
    }

    @Transactional(readOnly = true)
    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return mapToDTO(user);
    }

    @Transactional
    public UserDTO updateUser(Long id, UserCreateDTO dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (dto.getEmail() != null && !dto.getEmail().equals(user.getEmail()) && userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
        if (dto.getUsername() != null && !dto.getUsername().equals(user.getUsername()) && userRepository.existsByUsername(dto.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (dto.getFirstName() != null) user.setFirstName(dto.getFirstName());
        if (dto.getLastName() != null) user.setLastName(dto.getLastName());
        if (dto.getEmail() != null) user.setEmail(dto.getEmail());
        if (dto.getPassword() != null) user.setPassword(passwordEncoder.encode(dto.getPassword()));
        if (dto.getPhone() != null) user.setPhone(dto.getPhone());
        if (dto.getCity() != null) user.setCity(dto.getCity());
        if (dto.getUsername() != null) user.setUsername(dto.getUsername());

        userRepository.save(user);
        return mapToDTO(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("User not found");
        }
        userRepository.deleteById(id);
    }

    @Transactional
    public UserDTO confirmAccount(String token) {
        UserConfirmationToken confirmationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired token"));
        if (confirmationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Invalid or expired token");
        }
        User user = confirmationToken.getUser();
        user.setIsConfirmed(true);
        userRepository.save(user);
        tokenRepository.delete(confirmationToken);
        return mapToDTO(user);
    }

    @Transactional
    public void requestPasswordReset(String email) throws MessagingException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setToken(generateFourDigitToken());
        resetToken.setExpiresAt(LocalDateTime.now().plusHours(1)); // Expira en 1 hora
        passwordResetTokenRepository.save(resetToken);

        emailService.sendPasswordResetEmail(user.getEmail(), resetToken.getToken());
    }

    @Transactional
    public UserDTO resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired token"));
        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Invalid or expired token");
        }
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        passwordResetTokenRepository.delete(resetToken);
        return mapToDTO(user);
    }

    @Transactional
    public UserDTO changePassword(Long id, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return mapToDTO(user);
    }

    private UserDTO mapToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setCity(user.getCity());
        dto.setUsername(user.getUsername());
        dto.setIsConfirmed(user.getIsConfirmed());
        dto.setFcmToken(user.getFcmToken());
        return dto;
    }
}