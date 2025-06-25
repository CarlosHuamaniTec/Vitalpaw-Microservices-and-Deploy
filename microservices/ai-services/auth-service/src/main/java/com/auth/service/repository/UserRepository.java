package com.vitalpaw.auth.repository;

import com.vitalpaw.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {
    User findByApiKey(String apiKey);
}