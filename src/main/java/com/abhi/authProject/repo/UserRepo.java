package com.abhi.authProject.repo;

import com.abhi.authProject.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepo extends JpaRepository<Users, Integer> {

    // Find a user by their username
    Optional<Users> findByUsername(String username);

    // Find a user by their email
    Optional<Users> findByEmail(String email);

    // Find a user by their email verification token (OTP)
    Optional<Users> findByVerificationToken(String verificationToken);

    // --- NEW: Method to find a user by their password reset token ---
    Optional<Users> findByPasswordResetToken(String passwordResetToken);
}