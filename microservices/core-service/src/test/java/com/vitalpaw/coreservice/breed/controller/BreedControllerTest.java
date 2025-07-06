package com.vitalpaw.coreservice.breed.controller;

import com.vitalpaw.coreservice.breed.dto.BreedDTO;
import com.vitalpaw.coreservice.breed.service.BreedService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration; // Importar para excluir DB

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@WebMvcTest(controllers = BreedController.class, excludeAutoConfiguration = {DataSourceAutoConfiguration.class}) // ¡CORREGIDO! Excluir auto-configuración de DB
class BreedControllerTest {

    @Autowired
    private BreedController breedController;

    @MockBean
    private BreedService breedService;

    @Test
    void testCreateBreed() {
        BreedDTO dto = new BreedDTO();
        dto.setId(1L);
        dto.setName("Labrador");
        dto.setMinHeartRate(60);
        dto.setMaxHeartRate(120);
        dto.setMinTemperature(37.0f);
        dto.setMaxTemperature(39.5f);

        when(breedService.createBreed(dto)).thenReturn(dto);

        ResponseEntity<BreedDTO> response = breedController.createBreed(dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(dto, response.getBody());
    }

    @Test
    void testGetBreedById() {
        BreedDTO expected = new BreedDTO();
        expected.setId(1L);
        expected.setName("Labrador");
        expected.setMinHeartRate(60);
        expected.setMaxHeartRate(120);
        expected.setMinTemperature(37.0f);
        expected.setMaxTemperature(39.5f);

        when(breedService.getBreedById(1L)).thenReturn(expected);

        ResponseEntity<BreedDTO> response = breedController.getBreed(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
    }

    @Test
    void testGetAllBreeds() {
        BreedDTO breed = new BreedDTO();
        breed.setId(1L);
        breed.setName("Labrador");
        breed.setMinHeartRate(60);
        breed.setMaxHeartRate(120);
        breed.setMinTemperature(37.0f);
        breed.setMaxTemperature(39.5f);

        List<BreedDTO> breedList = List.of(breed);

        when(breedService.getAllBreeds()).thenReturn(breedList);

        ResponseEntity<List<BreedDTO>> response = breedController.getAllBreeds();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("Labrador", response.getBody().get(0).getName());
    }
}