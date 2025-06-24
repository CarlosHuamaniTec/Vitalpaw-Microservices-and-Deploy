package com.vitalpaw.coreservice.user.controller;

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

@RestController
@RequestMapping("/api/users")
@SecurityRequirement(name = "ApiKeyAuth")
public class UserController {

    @Autowired
    private UserService userService;

    @Operation(summary = "Crear un nuevo usuario", description = "Registra un nuevo usuario en el sistema.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Usuario creado exitosamente", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = UserDTO.class))
        }),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content),
        @ApiResponse(responseCode = "401", description = "Clave API inválida o faltante", content = @Content),
        @ApiResponse(responseCode = "409", description = "Correo o nombre de usuario duplicado", content = @Content),
        @ApiResponse(responseCode = "500", description = "Error al enviar el correo de confirmación", content = @Content)
    })
    @PostMapping
    public ResponseEntity<UserDTO> createUser(@Valid @RequestBody UserCreateDTO dto) {
        UserDTO createdUser = userService.createUser(dto);
        return ResponseEntity.ok(createdUser);
    }

    @Operation(summary = "Obtener usuario por ID", description = "Devuelve los datos de un usuario específico.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Datos del usuario devueltos", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = UserDTO.class))
        }),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado", content = @Content),
        @ApiResponse(responseCode = "401", description = "Clave API inválida o faltante", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @Operation(summary = "Actualizar usuario", description = "Modifica los datos de un usuario existente.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Usuario actualizado", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = UserDTO.class))
        }),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado", content = @Content),
        @ApiResponse(responseCode = "401", description = "Clave API inválida o faltante", content = @Content)
    })
    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateUser(@PathVariable Long id, @Valid @RequestBody UserCreateDTO dto) {
        return ResponseEntity.ok(userService.updateUser(id, dto));
    }

    @Operation(summary = "Eliminar usuario", description = "Elimina un usuario del sistema.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Usuario eliminado exitosamente"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado", content = @Content),
        @ApiResponse(responseCode = "401", description = "Clave API inválida o faltante", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Confirmar cuenta de usuario", description = "Confirma la cuenta del usuario usando un token.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cuenta confirmada exitosamente", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = UserDTO.class))
        }),
        @ApiResponse(responseCode = "400", description = "Token inválido o expirado", content = @Content)
    })
    @GetMapping("/confirm/{token}")
    public ResponseEntity<UserDTO> confirmAccount(@PathVariable String token) {
        UserDTO confirmedUser = userService.confirmAccount(token);
        return ResponseEntity.ok(confirmedUser);
    }

    @Operation(summary = "Solicitar restablecimiento de contraseña", description = "Envía un token de restablecimiento al correo del usuario.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token enviado exitosamente"),
        @ApiResponse(responseCode = "404", description = "Correo no encontrado", content = @Content),
        @ApiResponse(responseCode = "500", description = "Error al enviar el correo", content = @Content)
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

    @Operation(summary = "Restablecer contraseña", description = "Actualiza la contraseña usando un token enviado al correo.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Contraseña restablecida exitosamente", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = UserDTO.class))
        }),
        @ApiResponse(responseCode = "400", description = "Token inválido o expirado", content = @Content)
    })
    @PostMapping("/password-reset")
    public ResponseEntity<UserDTO> resetPassword(@RequestParam String token, @RequestParam String newPassword) {
        UserDTO updatedUser = userService.resetPassword(token, newPassword);
        return ResponseEntity.ok(updatedUser);
    }

    @Operation(summary = "Cambiar contraseña de usuario", description = "Actualiza la contraseña de un usuario autenticado.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Contraseña cambiada exitosamente", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = UserDTO.class))
        }),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado", content = @Content),
        @ApiResponse(responseCode = "401", description = "Clave API inválida o faltante", content = @Content)
    })
    @PostMapping("/{id}/change-password")
    public ResponseEntity<UserDTO> changePassword(@PathVariable Long id, @Valid @RequestBody PasswordChangeDTO dto) {
        return ResponseEntity.ok(userService.changePassword(id, dto.getNewPassword()));
    }
}