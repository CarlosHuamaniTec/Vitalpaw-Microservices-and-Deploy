package com.vitalpaw.coreservice.alert.controller;

import com.vitalpaw.coreservice.alert.dto.AlertDTO;
import com.vitalpaw.coreservice.alert.model.Alert;
import com.vitalpaw.coreservice.alert.repository.AlertRepository;
import com.vitalpaw.coreservice.pet.service.PetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/alerts")
@SecurityRequirement(name = "ApiKeyAuth") // Requiere API Key
public class AlertController {

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private PetService petService;

    @Operation(summary = "Obtener alertas por ID de mascota", description = "Devuelve las últimas alertas asociadas a una mascota.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de alertas devuelta", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = AlertDTO.class))
        }),
        @ApiResponse(responseCode = "404", description = "Mascota no encontrada", content = @Content),
        @ApiResponse(responseCode = "401", description = "Clave API inválida o faltante", content = @Content)
    })
    @GetMapping("/pet/{petId}")
    public ResponseEntity<List<AlertDTO>> getAlertsByPetId(
        @Parameter(description = "ID único de la mascota", required = true) @PathVariable Long petId,
        @RequestParam(defaultValue = "10") @Min(1) int limit) {

        // Verificar que la mascota exista
        petService.getPetById(petId);

        Pageable pageable = PageRequest.of(0, limit);
        List<Alert> alerts = alertRepository.findTopByPetId(petId, pageable);
        List<AlertDTO> alertDTOs = alerts.stream().map(this::mapToAlertDTO).collect(Collectors.toList());

        return ResponseEntity.ok(alertDTOs);
    }

    @Operation(summary = "Eliminar una alerta", description = "Elimina una alerta específica del sistema.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Alerta eliminada exitosamente"),
        @ApiResponse(responseCode = "404", description = "Alerta no encontrada", content = @Content),
        @ApiResponse(responseCode = "401", description = "Clave API inválida o faltante", content = @Content)
    })
    @DeleteMapping("/{alertId}")
    public ResponseEntity<Void> deleteAlert(
        @Parameter(description = "ID único de la alerta", required = true) @PathVariable Long alertId) {

        if (!alertRepository.existsById(alertId)) {
            return ResponseEntity.notFound().build();
        }

        alertRepository.deleteById(alertId);
        return ResponseEntity.noContent().build();
    }

    private AlertDTO mapToAlertDTO(Alert alert) {
        AlertDTO dto = new AlertDTO();
        dto.setId(alert.getId());
        dto.setPetId(alert.getPet().getId());
        dto.setMessage(alert.getMessage());
        dto.setTimestamp(alert.getTimestamp());
        dto.setType(alert.getType());
        dto.setSeverity(alert.getSeverity());
        dto.setPulse(alert.getPulse());
        dto.setTemperature(alert.getTemperature());
        return dto;
    }
}