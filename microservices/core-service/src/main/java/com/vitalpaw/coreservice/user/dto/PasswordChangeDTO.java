package com.vitalpaw.coreservice.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Datos necesarios para cambiar la contraseña de un usuario")
public class PasswordChangeDTO {

    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 8, max = 128, message = "La contraseña debe tener entre 8 y 128 caracteres")
    @Schema(example = "NuevaContrasena123!", description = "Nueva contraseña del usuario")
    private String newPassword;
}