package com.vitalpaw.coreservice.breed.controller;

import com.vitalpaw.coreservice.breed.dto.BreedDTO;
import com.vitalpaw.coreservice.breed.service.BreedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/breeds")
@SecurityRequirement(name = "ApiKeyAuth")
public class BreedController {
    @Autowired
    private BreedService breedService;

    @Operation(summary = "Crear una nueva raza", description = "Registra una nueva raza de mascota con umbrales de signos vitales.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Raza creada exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "401", description = "Clave API inválida o faltante"),
        @ApiResponse(responseCode = "409", description = "Nombre de raza duplicado")
    })
    @PostMapping
    public ResponseEntity<BreedDTO> createBreed(@Valid @RequestBody BreedDTO dto) {
        BreedDTO createdBreed = breedService.createBreed(dto);
        return ResponseEntity.ok(createdBreed);
    }

    @Operation(summary = "Obtener raza por ID", description = "Devuelve los detalles de una raza según su ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Raza encontrada"),
        @ApiResponse(responseCode = "401", description = "Clave API inválida o faltante"),
        @ApiResponse(responseCode = "404", description = "Raza no encontrada")
    })
    @GetMapping("/{id}")
    public ResponseEntity<BreedDTO> getBreed(@PathVariable Long id) {
        return ResponseEntity.ok(breedService.getBreedById(id));
    }

    @Operation(summary = "Listar todas las razas", description = "Devuelve una lista de todas las razas registradas con sus nombres y detalles.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de razas obtenida exitosamente"),
        @ApiResponse(responseCode = "401", description = "Clave API inválida o faltante")
    })
    @GetMapping
    public ResponseEntity<List<BreedDTO>> getAllBreeds() {
        return ResponseEntity.ok(breedService.getAllBreeds());
    }
}