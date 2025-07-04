package com.vitalpaw.coreservice.pet.controller;

import com.vitalpaw.coreservice.pet.dto.PetCreateDTO;
import com.vitalpaw.coreservice.pet.dto.PetDTO;
import com.vitalpaw.coreservice.pet.service.PetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Controlador para gestionar los endpoints relacionados con mascotas.
 */
@RestController
@RequestMapping("/api/pets")
@SecurityRequirement(name = "ApiKeyAuth")
public class PetController {

    @Autowired
    private PetService petService;

    @Value("${app.image.storage-path}")
    private String uploadDir;

    @Operation(summary = "Crear una nueva mascota", description = "Registra una nueva mascota en el sistema.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Mascota creada exitosamente", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = PetDTO.class))
        }),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content),
        @ApiResponse(responseCode = "401", description = "Clave API inválida o faltante", content = @Content)
    })
    @PostMapping
    public ResponseEntity<PetDTO> createPet(@Valid @RequestBody PetCreateDTO dto) {
        PetDTO createdPet = petService.createPet(dto);
        return ResponseEntity.ok(createdPet);
    }

    @Operation(summary = "Obtener mascota por ID", description = "Devuelve los datos de una mascota específica.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Datos de mascota devueltos", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = PetDTO.class))
        }),
        @ApiResponse(responseCode = "404", description = "Mascota no encontrada", content = @Content),
        @ApiResponse(responseCode = "401", description = "Clave API inválida o faltante", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<PetDTO> getPet(
        @Parameter(description = "ID único de la mascota", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(petService.getPetById(id));
    }

    @Operation(summary = "Obtener todas las mascotas de un usuario", description = "Devuelve una lista de todas las mascotas asociadas a un usuario por su ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de mascotas devuelta", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = PetDTO.class))
        }),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado", content = @Content),
        @ApiResponse(responseCode = "401", description = "Clave API inválida o faltante", content = @Content)
    })
    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<List<PetDTO>> getPetsByOwnerId(
        @Parameter(description = "ID único del usuario", required = true) @PathVariable Long ownerId) {
        return ResponseEntity.ok(petService.getPetsByOwnerId(ownerId));
    }

    @Operation(summary = "Actualizar mascota", description = "Modifica los datos de una mascota existente.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Mascota actualizada", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = PetDTO.class))
        }),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content),
        @ApiResponse(responseCode = "404", description = "Mascota no encontrada", content = @Content),
        @ApiResponse(responseCode = "401", description = "Clave API inválida o faltante", content = @Content)
    })
    @PutMapping("/{id}")
    public ResponseEntity<PetDTO> updatePet(
        @Parameter(description = "ID único de la mascota", required = true) @PathVariable Long id,
        @Valid @RequestBody PetCreateDTO dto) {
        return ResponseEntity.ok(petService.updatePet(id, dto));
    }

    @Operation(summary = "Eliminar una mascota", description = "Elimina una mascota específica del sistema según su ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Mascota eliminada exitosamente", content = @Content),
        @ApiResponse(responseCode = "404", description = "Mascota no encontrada", content = @Content),
        @ApiResponse(responseCode = "401", description = "Clave API inválida o faltante", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePet(
        @Parameter(description = "ID único de la mascota", required = true) @PathVariable Long id) {
        petService.deletePet(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Subir foto de mascota", description = "Sube una imagen para la mascota especificada.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Foto subida exitosamente", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = PetDTO.class))
        }),
        @ApiResponse(responseCode = "400", description = "Archivo inválido o demasiado grande", content = @Content),
        @ApiResponse(responseCode = "404", description = "Mascota no encontrada", content = @Content),
        @ApiResponse(responseCode = "401", description = "Clave API inválida o faltante", content = @Content),
        @ApiResponse(responseCode = "500", description = "Error al guardar la imagen", content = @Content)
    })
    @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PetDTO> uploadPetPhoto(
        @Parameter(description = "ID único de la mascota", required = true) @PathVariable Long id,
        @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(petService.uploadPetPhoto(id, file));
    }

    @Operation(summary = "Obtener foto de mascota", description = "Devuelve la imagen de la mascota especificada por su ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Imagen de la mascota devuelta", content = {
            @Content(mediaType = "image/jpeg", schema = @Schema(type = "string", format = "binary")),
            @Content(mediaType = "image/png", schema = @Schema(type = "string", format = "binary"))
        }),
        @ApiResponse(responseCode = "404", description = "Mascota o imagen no encontrada", content = @Content),
        @ApiResponse(responseCode = "401", description = "Clave API inválida o faltante", content = @Content)
    })
    @GetMapping(value = "/{id}/photo", produces = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE})
    public ResponseEntity<Resource> getPetPhoto(
        @Parameter(description = "ID único de la mascota", required = true) @PathVariable Long id) throws IOException {
        PetDTO pet = petService.getPetById(id);
        if (pet.getPhoto() == null || pet.getPhoto().isEmpty()) {
            throw new IllegalArgumentException("No se encontró una foto para la mascota con ID: " + id);
        }

        Path filePath = Paths.get(uploadDir).resolve(pet.getPhoto().replace("/images/pets/", ""));
        Resource resource = new UrlResource(filePath.toUri());
        if (!resource.exists() || !resource.isReadable()) {
            throw new IllegalArgumentException("No se encontró el archivo de la foto para la mascota con ID: " + id);
        }

        String contentType = pet.getPhoto().endsWith(".jpg") || pet.getPhoto().endsWith(".jpeg") ?
            MediaType.IMAGE_JPEG_VALUE : MediaType.IMAGE_PNG_VALUE;

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .body(resource);
    }
}