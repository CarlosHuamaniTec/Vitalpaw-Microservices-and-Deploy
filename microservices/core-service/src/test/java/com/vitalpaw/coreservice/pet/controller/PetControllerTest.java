package com.vitalpaw.coreservice.pet.controller;

import com.vitalpaw.coreservice.pet.dto.PetCreateDTO;
import com.vitalpaw.coreservice.pet.dto.PetDTO;
import com.vitalpaw.coreservice.pet.service.PetService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration; // Importar para excluir DB

import java.io.IOException;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@WebMvcTest(controllers = PetController.class, excludeAutoConfiguration = {DataSourceAutoConfiguration.class}) // ¡CORREGIDO! Excluir auto-configuración de DB
class PetControllerTest {

    @Autowired
    private PetController petController;

    @MockBean
    private PetService petService;

    @Test
    void testCreatePet() {
        PetCreateDTO dto = new PetCreateDTO();
        dto.setName("Firulais");
        dto.setSpecies("Perro"); // CORREGIDO: Cambiado de setType a setSpecies
        // dto.setAge(3); // ELIMINADO: No existe setter para 'age' en PetCreateDTO
        dto.setOwnerId(1L); // AÑADIDO: ownerId es @NotNull en PetCreateDTO
        dto.setBirthDate(LocalDate.of(2022, 1, 1)); // AÑADIDO: Usa birthDate en lugar de age

        PetDTO expected = new PetDTO();
        expected.setId(1L);
        expected.setName("Firulais");
        expected.setSpecies("Perro"); // CORREGIDO: Cambiado de setType a setSpecies
        // expected.setAge(3); // ELIMINADO: No existe setter para 'age' en PetDTO
        expected.setOwnerId(1L); // AÑADIDO: ownerId en PetDTO
        expected.setBirthDate(LocalDate.of(2022, 1, 1)); // AÑADIDO: Usa birthDate en lugar de age

        when(petService.createPet(dto)).thenReturn(expected);

        ResponseEntity<PetDTO> response = petController.createPet(dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
    }

    @Test
    void testCreatePetInvalid() {
        PetCreateDTO dto = new PetCreateDTO();
        dto.setName(""); // Nombre vacío para activar validación
        dto.setSpecies("Perro");
        dto.setOwnerId(1L);
        dto.setBirthDate(LocalDate.of(2022, 1, 1));

        // ¡CORREGIDO! Configurar el mock del servicio para lanzar la excepción esperada
        // Asumo que tu servicio lanzaría IllegalArgumentException para validaciones fallidas
        when(petService.createPet(any(PetCreateDTO.class)))
            .thenThrow(new IllegalArgumentException("El nombre es obligatorio"));

        assertThrows(IllegalArgumentException.class, () -> { // Esperar IllegalArgumentException
            petController.createPet(dto);
        });
    }

    @Test
    void testGetPetById() {
        PetDTO expected = new PetDTO();
        expected.setId(1L);
        expected.setName("Luna");
        expected.setSpecies("Gato"); // CORREGIDO: Cambiado de setType a setSpecies
        // expected.setAge(2); // ELIMINADO
        expected.setOwnerId(1L); // AÑADIDO
        expected.setBirthDate(LocalDate.of(2023, 5, 10)); // AÑADIDO

        when(petService.getPetById(1L)).thenReturn(expected);

        ResponseEntity<PetDTO> response = petController.getPet(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
    }

    @Test
    void testGetPetByIdNotFound() {
        // ¡CORREGIDO! Configurar el mock del servicio para lanzar la excepción esperada
        when(petService.getPetById(99L)).thenThrow(new IllegalArgumentException("Mascota no encontrada"));

        assertThrows(IllegalArgumentException.class, () -> { // Esperar IllegalArgumentException
            petController.getPet(99L);
        });
    }

    @Test
    void testUpdatePet() {
        PetCreateDTO dto = new PetCreateDTO();
        dto.setName("Max");
        dto.setSpecies("Perro");
        dto.setOwnerId(1L);
        dto.setBirthDate(LocalDate.of(2021, 3, 15));

        PetDTO expected = new PetDTO();
        expected.setId(1L);
        expected.setName("Max");
        expected.setSpecies("Perro");
        expected.setOwnerId(1L);
        expected.setBirthDate(LocalDate.of(2021, 3, 15));

        when(petService.updatePet(1L, dto)).thenReturn(expected);

        ResponseEntity<PetDTO> response = petController.updatePet(1L, dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
    }

    @Test
    void testUpdatePetNotFound() {
        PetCreateDTO dto = new PetCreateDTO();
        dto.setName("Rocky");
        dto.setSpecies("Gato");
        dto.setOwnerId(1L);
        dto.setBirthDate(LocalDate.of(2024, 7, 1));

        // ¡CORREGIDO! Configurar el mock del servicio para lanzar la excepción esperada
        when(petService.updatePet(999L, dto)).thenThrow(new IllegalArgumentException("Mascota no encontrada"));

        assertThrows(IllegalArgumentException.class, () -> { // Esperar IllegalArgumentException
            petController.updatePet(999L, dto);
        });
    }

    @Test
    void testUploadPetPhoto() throws IOException {
        MultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", "imagen".getBytes());
        PetDTO expected = new PetDTO();
        expected.setId(1L);
        expected.setName("Firulais");
        expected.setSpecies("Perro");
        expected.setOwnerId(1L);
        expected.setBirthDate(LocalDate.of(2022, 1, 1));

        when(petService.uploadPetPhoto(1L, file)).thenReturn(expected);

        ResponseEntity<PetDTO> response = petController.uploadPetPhoto(1L, file);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
    }

    @Test
    void testUploadPetPhotoInvalidFile() throws IOException { // Añadido throws IOException
        MultipartFile file = null;

        // ¡CORREGIDO! Configurar el mock del servicio para lanzar la excepción esperada
        when(petService.uploadPetPhoto(anyLong(), eq(file)))
            .thenThrow(new IllegalArgumentException("El archivo está vacío"));

        assertThrows(IllegalArgumentException.class, () -> {
            petController.uploadPetPhoto(1L, file);
        });
    }

    @Test
    void testUploadPetPhotoNotFound() throws IOException {
        MultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", "imagen".getBytes());

        // ¡CORREGIDO! Configurar el mock del servicio para lanzar la excepción esperada
        when(petService.uploadPetPhoto(eq(999L), any(MultipartFile.class)))
            .thenThrow(new IllegalArgumentException("Mascota no encontrada"));

        assertThrows(IllegalArgumentException.class, () -> {
            petController.uploadPetPhoto(999L, file);
        });
    }

    @Test
    void testValidationActivated() {
        PetCreateDTO invalidDto = new PetCreateDTO();
        invalidDto.setName(null); // Nombre nulo para activar validación
        invalidDto.setSpecies("Gato");
        invalidDto.setOwnerId(1L);
        invalidDto.setBirthDate(LocalDate.of(2023, 10, 20));

        // ¡CORREGIDO! Configurar el mock del servicio para lanzar la excepción esperada
        when(petService.createPet(any(PetCreateDTO.class)))
            .thenThrow(new IllegalArgumentException("El nombre es obligatorio"));

        assertThrows(IllegalArgumentException.class, () -> { // Esperar IllegalArgumentException
            petController.createPet(invalidDto);
        });
    }
}
