package com.abhi.authProject.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime; // Import for LocalDateTime

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class Users {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private int id;
    private String username;
    private String password;
    private String email;
    private String role;
    private boolean isVerified; // NEW FIELD: Default to false for new registrations
    private String verificationToken; // NEW FIELD: To store the unique token
    private LocalDateTime verificationTokenExpires; // NEW FIELD: To store token expiry time
}