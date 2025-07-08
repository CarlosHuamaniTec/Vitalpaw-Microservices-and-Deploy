package com.vitalpaw.coreservice.user.controller;

import com.vitalpaw.coreservice.user.dto.UserDTO;
import com.vitalpaw.coreservice.user.dto.UserCreateDTO;
import com.vitalpaw.coreservice.user.service.UserService;
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

@WebMvcTest(controllers = UserController.class, excludeAutoConfiguration = {DataSourceAutoConfiguration.class}) // ¡CORREGIDO! Excluir auto-configuración de DB
class UserControllerTest {

    @Autowired
    private UserController userController;

    @MockBean
    private UserService userService;

    @Test
    void testCreateUser() {
        UserCreateDTO userCreateDTO = new UserCreateDTO();
        userCreateDTO.setUsername("testuser");
        userCreateDTO.setEmail("test@example.com");
        userCreateDTO.setPassword("password123");
        userCreateDTO.setFirstName("Test"); // Añadido para cumplir con @NotBlank
        userCreateDTO.setLastName("User");  // Añadido para completar el DTO

        UserDTO expectedUserDTO = new UserDTO();
        expectedUserDTO.setId(1L);
        expectedUserDTO.setUsername("testuser");
        expectedUserDTO.setEmail("test@example.com");
        expectedUserDTO.setFirstName("Test"); // Añadido
        expectedUserDTO.setLastName("User");  // Añadido

        when(userService.createUser(any(UserCreateDTO.class))).thenReturn(expectedUserDTO);

        ResponseEntity<UserDTO> response = userController.createUser(userCreateDTO);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedUserDTO, response.getBody());
    }

    // Puedes añadir más pruebas aquí para otros métodos de UserController
    // Por ejemplo:
    /*
    @Test
    void testGetUserById() {
        UserDTO expected = new UserDTO();
        expected.setId(1L);
        expected.setFirstName("Carlos");
        expected.setLastName("Huamani");
        expected.setEmail("carlos@example.com");
        expected.setUsername("carloshuamani");
        expected.setIsConfirmed(true);

        when(userService.getUserById(1L)).thenReturn(expected);

        ResponseEntity<UserDTO> response = userController.getUser(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
    }

    @Test
    void testLogin() {
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setEmail("test@example.com");
        loginDTO.setPassword("password123");

        UserDTO expectedUserDTO = new UserDTO();
        expectedUserDTO.setId(1L);
        expectedUserDTO.setEmail("test@example.com");
        expectedUserDTO.setUsername("testuser");

        when(userService.login(loginDTO)).thenReturn(expectedUserDTO);

        ResponseEntity<UserDTO> response = userController.login(loginDTO);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedUserDTO, response.getBody());
    }
    */
}