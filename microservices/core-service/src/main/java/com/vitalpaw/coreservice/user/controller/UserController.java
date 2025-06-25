package com.vitalpaw.coreservice.user.controller;

import com.vitalpaw.coreservice.user.dto.LoginDTO;
import com.vitalpaw.coreservice.user.dto.PasswordChangeDTO;
import com.vitalpaw.coreservice.user.dto.UserCreateDTO;
import com.vitalpaw.coreservice.user.dto.UserDTO;
import com.vitalpaw.coreservice.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador para gestionar los endpoints relacionados con usuarios.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Operation(summary = "Iniciar sesión", description = "Autentica a un usuario en el sistema verificando su correo electrónico y contraseña.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuario autenticado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDTO.class))),
            @ApiResponse(responseCode = "400", description = "Credenciales inválidas o cuenta no confirmada",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content)
    })
    @PostMapping("/login")
    public ResponseEntity<UserDTO> login(@Valid @RequestBody LoginDTO loginDTO) {
        UserDTO user = userService.login(loginDTO);
        return ResponseEntity.ok(user);
    }

    @Operation(summary = "Crear un nuevo usuario", description = "Registra un nuevo usuario en el sistema.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuario registrado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Clave API inválida o faltante",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "Correo o nombre de usuario duplicado",
                    content = @Content),
            @ApiResponse(responseCode = "500", description = "Error al enviar el correo electrónico de confirmación",
                    content = @Content)
    })
    @SecurityRequirement(name = "ApiKeyAuth")
    @PostMapping
    public ResponseEntity<UserDTO> createUser(@Valid @RequestBody UserCreateDTO dto) {
        UserDTO createdUser = userService.createUser(dto);
        return ResponseEntity.ok(createdUser);
    }

    @Operation(summary = "Obtener un usuario por ID", description = "Devuelve los datos de un usuario específico.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Datos del usuario retornados correctamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDTO.class))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Clave API inválida o faltante",
                    content = @Content)
    })
    @SecurityRequirement(name = "ApiKeyAuth")
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @Operation(summary = "Actualizar un usuario", description = "Modifica los datos de un usuario existente.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuario actualizado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Clave API inválida o faltante",
                    content = @Content)
    })
    @SecurityRequirement(name = "ApiKeyAuth")
    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateUser(@PathVariable Long id, @Valid @RequestBody UserCreateDTO userDTO) {
        return ResponseEntity.ok(userService.updateUser(id, userDTO));
    }

    @Operation(summary = "Eliminar un usuario", description = "Elimina un usuario del sistema.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Usuario eliminado exitosamente",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Clave API inválida o faltante",
                    content = @Content)
    })
    @SecurityRequirement(name = "ApiKeyAuth")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Confirmar la cuenta de un usuario", description = "Confirma la cuenta del usuario utilizando un token de confirmación.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cuenta confirmada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDTO.class))),
            @ApiResponse(responseCode = "400", description = "Token inválido o expirado",
                    content = @Content)
    })
    @GetMapping("/confirm/{token}")
    public ResponseEntity<UserDTO> confirmAccount(@PathVariable String token) {
        UserDTO confirmedUser = userService.confirmAccount(token);
        return ResponseEntity.ok(confirmedUser);
    }

    @Operation(summary = "Solicitud de restablecimiento de contraseña", description = "Envía un token de restablecimiento al correo electrónico del usuario.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Solicitud procesada y token enviado correctamente",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Correo electrónico no encontrado",
                    content = @Content),
            @ApiResponse(responseCode = "500", description = "Error al enviar el correo electrónico",
                    content = @Content)
    })
    @PostMapping("/password-reset/request")
    public ResponseEntity<Void> requestPasswordReset(@RequestParam String email) {
        try {
            userService.requestPasswordReset(email);
            return ResponseEntity.ok().build();
        } catch (MessagingException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Restablecer la contraseña", description = "Actualiza la contraseña utilizando el token enviado al correo electrónico.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Contraseña restablecida exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDTO.class))),
            @ApiResponse(responseCode = "400", description = "Token inválido o expirado",
                    content = @Content)
    })
    @PostMapping("/password-reset")
    public ResponseEntity<UserDTO> resetPassword(@RequestParam String token, @RequestParam String newPassword) {
        UserDTO updatedUser = userService.resetPassword(token, newPassword);
        return ResponseEntity.ok(updatedUser);
    }

    @Operation(summary = "Cambiar la contraseña de un usuario", description = "Actualiza la contraseña de un usuario autenticado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Contraseña cambiada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Clave API inválida o faltante",
                    content = @Content)
    })
    @SecurityRequirement(name = "ApiKeyAuth")
    @PostMapping("/{id}/change-password")
    public ResponseEntity<UserDTO> changePassword(@PathVariable Long id, @Valid @RequestBody PasswordChangeDTO dto) {
        return ResponseEntity.ok(userService.changePassword(id, dto.getNewPassword()));
    }

    @Operation(summary = "Obtener el token de confirmación (solo para pruebas)", description = "Devuelve el token de confirmación para un usuario específico.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token devuelto exitosamente",
                    content = @Content(mediaType = "text/plain")),
            @ApiResponse(responseCode = "404", description = "Usuario o token no encontrado",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Clave API inválida o faltante",
                    content = @Content)
    })
    @SecurityRequirement(name = "ApiKeyAuth")
    @GetMapping("/confirmation-token/{userId}")
    public ResponseEntity<String> getConfirmationToken(@PathVariable Long userId) {
        String token = userService.getConfirmationToken(userId);
        return ResponseEntity.ok(token);
    }

    @Operation(summary = "Obtener el token de restablecimiento de contraseña (solo para pruebas)", description = "Devuelve el token de restablecimiento para un usuario específico.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token devuelto exitosamente",
                    content = @Content(mediaType = "text/plain")),
            @ApiResponse(responseCode = "404", description = "Usuario o token no encontrado",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Clave API inválida o faltante",
                    content = @Content)
    })
    @SecurityRequirement(name = "ApiKeyAuth")
    @GetMapping("/password-reset-token/{userId}")
    public ResponseEntity<String> getPasswordResetToken(@PathVariable Long userId) {
        String token = userService.getPasswordResetToken(userId);
        return ResponseEntity.ok(token);
    }

    @Operation(summary = "Actualizar el token FCM", description = "Actualiza el token FCM para notificaciones push del usuario.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token FCM actualizado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDTO.class))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Clave API inválida o faltante",
                    content = @Content)
    })
    @SecurityRequirement(name = "ApiKeyAuth")
    @PutMapping("/{id}/fcm-token")
    public ResponseEntity<UserDTO> updateFcmToken(@PathVariable Long id, @RequestParam String fcmToken) {
        UserDTO updatedUser = userService.updateFcmToken(id, fcmToken);
        return ResponseEntity.ok(updatedUser);
    }
}