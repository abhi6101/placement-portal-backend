package com.abhi.authProject.repo;

import com.abhi.authProject.model.PasswordResetToken;
import com.abhi.authProject.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepo extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    Optional<PasswordResetToken> findByUser(Users user);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    void deleteByUser(Users user);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(value = "DELETE FROM password_reset_tokens WHERE user_id = ?1", nativeQuery = true)
    @org.springframework.transaction.annotation.Transactional
    void deleteByUserId(Long userId);

    void deleteByExpiryDateBefore(LocalDateTime now);
}
