package com.abhi.authProject.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@AllArgsConstructor // Generates constructor with all fields
@NoArgsConstructor  // Generates no-argument constructor
@Table(name = "users")
public class Users {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private int id;
    private String username;
    private String password;
    private String email;
    private String role;

    // --- Email Verification Fields ---
    @Column(name = "is_verified", nullable = false, columnDefinition = "boolean default false")
    private boolean isVerified = false; // Initialize to false directly

    private String verificationToken; // Stores the OTP code for email verification
    private LocalDateTime verificationTokenExpires; // Expiry for the OTP code

    // --- NEW: Password Reset Fields ---
    // This token will be a UUID, used for 'forgot password' functionality
    @Column(name = "password_reset_token")
    private String passwordResetToken;

    // This stores the expiration time for the password reset token
    @Column(name = "password_reset_token_expires")
    private LocalDateTime passwordResetTokenExpires;

    // Important consideration for Lombok's @AllArgsConstructor:
    // If you're relying solely on @AllArgsConstructor for constructors,
    // when you add new fields, you need to make sure your code that calls
    // this constructor (e.g., in tests or other services) is updated
    // to include the new fields.
    // If you use @Builder, ensure new fields are handled correctly there too.
    // For general use, @Data, @AllArgsConstructor, @NoArgsConstructor are often sufficient.
}