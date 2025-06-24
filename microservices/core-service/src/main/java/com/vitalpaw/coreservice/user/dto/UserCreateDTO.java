package com.vitalpaw.coreservice.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@Schema(description = "Datos necesarios para crear un nuevo usuario")
public class UserCreateDTO {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder los 100 caracteres")
    @Schema(example = "Carlos", description = "Nombre del usuario")
    private String firstName;

    @Size(max = 100, message = "El apellido no puede exceder los 100 caracteres")
    @Schema(example = "Huamani", description = "Apellido del usuario (opcional)")
    private String lastName;

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "Debe ser un correo electrónico válido")
    @Schema(example = "carlos@example.com", description = "Correo electrónico único del usuario")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 128, message = "La contraseña debe tener entre 8 y 128 caracteres")
    @Schema(example = "SecurePass123!", description = "Contraseña del usuario")
    private String password;

    @Size(max = 20, message = "El número de teléfono no puede exceder los 20 dígitos")
    @Schema(example = "+51987654321", description = "Número de teléfono del usuario (opcional)")
    private String phone;

    @Size(max = 100, message = "La ciudad no puede exceder los 100 caracteres")
    @Schema(example = "Lima", description = "Ciudad donde vive el usuario (opcional)")
    private String city;

    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(max = 50, message = "El nombre de usuario no puede exceder los 50 caracteres")
    @Schema(example = "carloshuamani", description = "Nombre de usuario único")
    private String username;
}