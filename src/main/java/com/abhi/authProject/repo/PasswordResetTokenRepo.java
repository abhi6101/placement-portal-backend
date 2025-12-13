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

    void deleteByUser(Users user);

    void deleteByExpiryDateBefore(LocalDateTime now);
}
