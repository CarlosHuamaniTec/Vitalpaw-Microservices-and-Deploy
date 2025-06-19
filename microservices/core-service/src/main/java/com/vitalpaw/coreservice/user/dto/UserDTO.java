package com.vitalpaw.coreservice.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Datos públicos de un usuario")
public class UserDTO {

    @Schema(example = "1", description = "ID único del usuario")
    private Long id;

    @Schema(example = "Carlos", description = "Nombre del usuario")
    private String firstName;

    @Schema(example = "Huamani", description = "Apellido del usuario")
    private String lastName;

    @Schema(example = "carlos@example.com", description = "Correo electrónico del usuario")
    private String email;

    @Schema(example = "+51987654321", description = "Teléfono del usuario")
    private String phone;

    @Schema(example = "Lima", description = "Ciudad donde vive el usuario")
    private String city;

    @Schema(example = "carloshuamani", description = "Nombre de usuario único")
    private String username;

    @Schema(example = "true", description = "Indica si la cuenta fue confirmada")
    private Boolean isConfirmed;
}