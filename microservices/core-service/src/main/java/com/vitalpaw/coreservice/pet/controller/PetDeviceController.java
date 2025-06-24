package com.vitalpaw.coreservice.pet.controller;

import com.vitalpaw.coreservice.pet.dto.PetDeviceDTO;
import com.vitalpaw.coreservice.pet.service.PetDeviceService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/devices")
public class PetDeviceController {
    @Autowired
    private PetDeviceService petDeviceService;

    @PostMapping
    public ResponseEntity<PetDeviceDTO> createPetDevice(@Valid @RequestBody PetDeviceDTO dto) {
        PetDeviceDTO createdDevice = petDeviceService.createPetDevice(dto);
        return ResponseEntity.ok(createdDevice);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PetDeviceDTO> getPetDevice(@PathVariable Long id) {
        return ResponseEntity.ok(petDeviceService.getPetDeviceById(id));
    }

    @GetMapping("/device/{deviceId}")
    public ResponseEntity<PetDeviceDTO> getPetDeviceByDeviceId(@PathVariable String deviceId) {
        return ResponseEntity.ok(petDeviceService.getPetDeviceByDeviceId(deviceId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PetDeviceDTO> updatePetDevice(@PathVariable Long id, @Valid @RequestBody PetDeviceDTO dto) {
        return ResponseEntity.ok(petDeviceService.updatePetDevice(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePetDevice(@PathVariable Long id) {
        petDeviceService.deletePetDevice(id);
        return ResponseEntity.noContent().build();
    }
}