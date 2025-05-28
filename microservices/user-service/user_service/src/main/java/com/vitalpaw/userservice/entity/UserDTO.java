package com.vitalpaw.userservice.dto;

import lombok.Data;

@Data
public class UserDTO {
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String phone;
    private String city;
    private String username;
}