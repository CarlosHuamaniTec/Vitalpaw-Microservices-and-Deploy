package com.auth.service.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "api_keys")
@Data
public class Auth {
    @Id
    private String apiKey;
}