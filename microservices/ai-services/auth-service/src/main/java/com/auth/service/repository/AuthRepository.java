package com.auth.service.repository;

import com.auth.service.model.Auth;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AuthRepository extends JpaRepository<Auth, String> {
    Optional<Auth> findByApiKey(String apiKey);
}