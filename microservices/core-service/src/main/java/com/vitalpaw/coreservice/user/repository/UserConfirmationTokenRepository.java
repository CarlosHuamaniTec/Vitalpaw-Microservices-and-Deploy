package com.vitalpaw.coreservice.user.repository;

import com.vitalpaw.coreservice.user.model.UserConfirmationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserConfirmationTokenRepository extends JpaRepository<UserConfirmationToken, Long> {
    Optional<UserConfirmationToken> findByToken(String token);
}