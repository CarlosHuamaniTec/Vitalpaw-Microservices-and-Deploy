package com.vitalpaw.coreservice.user.service;

import com.vitalpaw.coreservice.user.dto.LoginDTO;
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

/**
 * Servicio para gestionar la lógica de negocio relacionada con usuarios.
 */
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

    /**
     * Genera un token de cuatro dígitos aleatorio.
     * @return Token de cuatro dígitos como cadena.
     */
    private String generateFourDigitToken() {
        return String.format("%04d", RANDOM.nextInt(10000));
    }

    /**
     * Autentica a un usuario verificando su correo electrónico, contraseña y estado de confirmación.
     * @param loginDTO Objeto con las credenciales de inicio de sesión.
     * @return DTO con los datos del usuario autenticado.
     * @throws IllegalArgumentException si las credenciales son inválidas o la cuenta no está confirmada.
     */
    @Transactional(readOnly = true)
    public UserDTO login(LoginDTO loginDTO) {
        User user = userRepository.findByEmail(loginDTO.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Correo electrónico o contraseña inválidos"));

        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Correo electrónico o contraseña inválidos");
        }

        if (!user.getIsConfirmed()) {
            throw new IllegalArgumentException("La cuenta no ha sido confirmada");
        }

        return mapToDTO(user);
    }

    /**
     * Crea un nuevo usuario y envía un correo de confirmación.
     * @param dto Objeto con los datos del usuario a crear.
     * @return DTO con los datos del usuario creado.
     * @throws IllegalArgumentException si el correo o nombre de usuario ya existen.
     * @throws RuntimeException si falla el envío del correo de confirmación.
     */
    @Transactional
    public UserDTO createUser(UserCreateDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("El correo electrónico ya existe");
        }
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new IllegalArgumentException("El nombre de usuario ya existe");
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
            throw new RuntimeException("Error al enviar el correo de confirmación", e);
        }

        return mapToDTO(user);
    }

    /**
     * Obtiene los datos de un usuario por su ID.
     * @param id Identificador único del usuario.
     * @return DTO con los datos del usuario.
     * @throws IllegalArgumentException si el usuario no existe.
     */
    @Transactional(readOnly = true)
    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        return mapToDTO(user);
    }

    /**
     * Actualiza los datos de un usuario existente.
     * @param id Identificador único del usuario.
     * @param dto Objeto con los nuevos datos del usuario.
     * @return DTO con los datos del usuario actualizado.
     * @throws IllegalArgumentException si el usuario no existe o el correo/nombre de usuario ya están en uso.
     */
    @Transactional
    public UserDTO updateUser(Long id, UserCreateDTO dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (dto.getEmail() != null && !dto.getEmail().equals(user.getEmail()) && userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("El correo electrónico ya existe");
        }
        if (dto.getUsername() != null && !dto.getUsername().equals(user.getUsername()) && userRepository.existsByUsername(dto.getUsername())) {
            throw new IllegalArgumentException("El nombre de usuario ya existe");
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

    /**
     * Elimina un usuario del sistema.
     * @param id Identificador único del usuario.
     * @throws IllegalArgumentException si el usuario no existe.
     */
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("Usuario no encontrado");
        }
        userRepository.deleteById(id);
    }

    /**
     * Confirma la cuenta de un usuario utilizando un token.
     * @param token Token de confirmación.
     * @return DTO con los datos del usuario confirmado.
     * @throws IllegalArgumentException si el token es inválido o ha expirado.
     */
    @Transactional
    public UserDTO confirmAccount(String token) {
        UserConfirmationToken confirmationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token inválido o expirado"));
        if (confirmationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Token inválido o expirado");
        }
        User user = confirmationToken.getUser();
        user.setIsConfirmed(true);
        userRepository.save(user);
        tokenRepository.delete(confirmationToken);
        return mapToDTO(user);
    }

    /**
     * Solicita el restablecimiento de contraseña enviando un token al correo del usuario.
     * @param email Correo electrónico del usuario.
     * @throws IllegalArgumentException si el correo no está registrado.
     * @throws MessagingException si falla el envío del correo.
     */
    @Transactional
    public void requestPasswordReset(String email) throws MessagingException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con el correo: " + email));

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setToken(generateFourDigitToken());
        resetToken.setExpiresAt(LocalDateTime.now().plusHours(1)); // Expira en 1 hora
        passwordResetTokenRepository.save(resetToken);

        emailService.sendPasswordResetEmail(user.getEmail(), resetToken.getToken());
    }

    /**
     * Restablece la contraseña de un usuario utilizando un token.
     * @param token Token de restablecimiento.
     * @param newPassword Nueva contraseña.
     * @return DTO con los datos del usuario actualizado.
     * @throws IllegalArgumentException si el token es inválido o ha expirado.
     */
    @Transactional
    public UserDTO resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token inválido o expirado"));
        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Token inválido o expirado");
        }
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        passwordResetTokenRepository.delete(resetToken);
        return mapToDTO(user);
    }

    /**
     * Cambia la contraseña de un usuario existente.
     * @param id Identificador único del usuario.
     * @param newPassword Nueva contraseña.
     * @return DTO con los datos del usuario actualizado.
     * @throws IllegalArgumentException si el usuario no existe.
     */
    @Transactional
    public UserDTO changePassword(Long id, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return mapToDTO(user);
    }

    /**
     * Obtiene el token de confirmación para un usuario (solo para pruebas).
     * @param userId Identificador único del usuario.
     * @return Token de confirmación.
     * @throws IllegalArgumentException si el token no existe.
     */
    @Transactional(readOnly = true)
    public String getConfirmationToken(Long userId) {
        UserConfirmationToken token = tokenRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Token de confirmación no encontrado para el usuario: " + userId));
        return token.getToken();
    }

    /**
     * Obtiene el token de restablecimiento de contraseña para un usuario (solo para pruebas).
     * @param userId Identificador único del usuario.
     * @return Token de restablecimiento.
     * @throws IllegalArgumentException si el token no existe.
     */
    @Transactional(readOnly = true)
    public String getPasswordResetToken(Long userId) {
        PasswordResetToken token = passwordResetTokenRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Token de restablecimiento no encontrado para el usuario: " + userId));
        return token.getToken();
    }

    /**
     * Actualiza el token FCM de un usuario para notificaciones push.
     * @param id Identificador único del usuario.
     * @param fcmToken Nuevo token FCM.
     * @return DTO con los datos del usuario actualizado.
     * @throws IllegalArgumentException si el usuario no existe.
     */
    @Transactional
    public UserDTO updateFcmToken(Long id, String fcmToken) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        user.setFcmToken(fcmToken);
        userRepository.save(user);
        return mapToDTO(user);
    }

    /**
     * Convierte un modelo de usuario a un DTO.
     * @param user Modelo de usuario.
     * @return DTO con los datos del usuario.
     */
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