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

    @Operation(summary = "Create a new breed", description = "Registers a new pet breed with vital sign thresholds.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Breed created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "401", description = "Invalid or missing API Key"),
            @ApiResponse(responseCode = "409", description = "Duplicate breed name")
    })
    @PostMapping
    public ResponseEntity<BreedDTO> createBreed(@Valid @RequestBody BreedDTO dto) {
        BreedDTO createdBreed = breedService.createBreed(dto);
        return ResponseEntity.ok(createdBreed);
    }

    @Operation(summary = "Get breed by ID", description = "Retrieves the details of a breed by its ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Breed found"),
            @ApiResponse(responseCode = "401", description = "Invalid or missing API Key"),
            @ApiResponse(responseCode = "404", description = "Breed not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<BreedDTO> getBreed(@PathVariable Long id) {
        return ResponseEntity.ok(breedService.getBreedById(id));
    }

    @Operation(summary = "List all breeds", description = "Retrieves a list of all registered breeds with their names and details.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of breeds retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid or missing API Key")
    })
    @GetMapping
    public ResponseEntity<List<BreedDTO>> getAllBreeds() {
        return ResponseEntity.ok(breedService.getAllBreeds());
    }
}