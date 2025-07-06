package com.vitalpaw.coreservice.pet.controller; // ¡IMPORTANTE! Asegúrate de que el archivo esté en esta ruta

import com.vitalpaw.coreservice.pet.dto.PetDeviceDTO; // Importa desde pet.dto
import com.vitalpaw.coreservice.pet.service.PetDeviceService; // Importa desde pet.service
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration; // Importar para excluir DB

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Asegúrate de que PetDeviceController se refiera a la clase en com.vitalpaw.coreservice.pet.controller
@WebMvcTest(controllers = com.vitalpaw.coreservice.pet.controller.PetDeviceController.class, excludeAutoConfiguration = {DataSourceAutoConfiguration.class}) // ¡CORREGIDO! Excluir auto-configuración de DB
class PetDeviceControllerTest {

    @Autowired
    // Aquí el tipo PetDeviceController se resolverá al de com.vitalpaw.coreservice.pet.controller
    private com.vitalpaw.coreservice.pet.controller.PetDeviceController petDeviceController;

    @MockBean
    private PetDeviceService petDeviceService;

    @Test
    void testCreatePetDevice() {
        PetDeviceDTO dto = new PetDeviceDTO();
        dto.setDeviceId("ESP32-01");
        dto.setPetId(1L);

        PetDeviceDTO expected = new PetDeviceDTO();
        expected.setId(1L);
        expected.setDeviceId("ESP32-01");
        expected.setPetId(1L);

        when(petDeviceService.createPetDevice(dto)).thenReturn(expected);

        ResponseEntity<PetDeviceDTO> response = petDeviceController.createPetDevice(dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
    }

    @Test
    void testGetPetDeviceById() {
        PetDeviceDTO expected = new PetDeviceDTO();
        expected.setId(1L);
        expected.setDeviceId("ESP32-01");
        expected.setPetId(1L);

        when(petDeviceService.getPetDeviceById(1L)).thenReturn(expected);

        ResponseEntity<PetDeviceDTO> response = petDeviceController.getPetDevice(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
    }

    @Test
    void testGetPetDeviceByDeviceId() {
        PetDeviceDTO expected = new PetDeviceDTO();
        expected.setId(1L);
        expected.setDeviceId("ESP32-01");
        expected.setPetId(1L);

        when(petDeviceService.getPetDeviceByDeviceId("ESP32-01")).thenReturn(expected);

        ResponseEntity<PetDeviceDTO> response = petDeviceController.getPetDeviceByDeviceId("ESP32-01");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
    }

    @Test
    void testUpdatePetDevice() {
        PetDeviceDTO dto = new PetDeviceDTO();
        dto.setDeviceId("ESP32-Updated");
        dto.setPetId(2L);

        PetDeviceDTO updated = new PetDeviceDTO();
        updated.setId(1L);
        updated.setDeviceId("ESP32-Updated");
        updated.setPetId(2L);

        when(petDeviceService.updatePetDevice(1L, dto)).thenReturn(updated);

        ResponseEntity<PetDeviceDTO> response = petDeviceController.updatePetDevice(1L, dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(updated, response.getBody());
    }

    @Test
    void testDeletePetDevice() {
        doNothing().when(petDeviceService).deletePetDevice(1L);

        ResponseEntity<Void> response = petDeviceController.deletePetDevice(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
    }
}
