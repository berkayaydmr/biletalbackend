package com.biletal.biletalbackend.repository;

import com.biletal.biletalbackend.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);
    Optional<PasswordResetToken> findByEmailAndUsedFalseAndExpiryDateAfter(String email, LocalDateTime now);
    void deleteByExpiryDateBefore(LocalDateTime now);
}
