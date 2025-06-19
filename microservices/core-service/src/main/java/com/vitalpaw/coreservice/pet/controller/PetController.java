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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/pets")
@SecurityRequirement(name = "ApiKeyAuth")
public class PetController {

    @Autowired
    private PetService petService;

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
}