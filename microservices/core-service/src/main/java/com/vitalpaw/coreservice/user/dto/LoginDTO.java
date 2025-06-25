package com.vitalpaw.coreservice.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Objeto de transferencia de datos para las credenciales de inicio de sesión.
 */
@Data
@Schema(description = "Datos necesarios para iniciar sesión")
public class LoginDTO {

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "Debe ser un correo electrónico válido")
    @Schema(example = "carlos@example.com", description = "Correo electrónico del usuario")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 128, message = "La contraseña debe tener entre 8 y 128 caracteres")
    @Schema(example = "SecurePass123!", description = "Contraseña del usuario")
    private String password;
}