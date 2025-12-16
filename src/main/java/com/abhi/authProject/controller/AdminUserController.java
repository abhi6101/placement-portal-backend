package com.abhi.authProject.controller;

import com.abhi.authProject.model.UserDto; // Import the new DTO
import com.abhi.authProject.model.Users;
import com.abhi.authProject.repo.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors; // Import collectors

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'COMPANY_ADMIN')") // Allow View for Company Admin
public class AdminUserController {

    @Autowired
    private com.abhi.authProject.repo.UserRepo userRepo;

    @Autowired
    private com.abhi.authProject.service.EmailService emailService;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @GetMapping("/users")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<Users> users = userRepo.findAll();

        List<UserDto> userDtos = users.stream()
                .map(user -> new UserDto(
                        user.getId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getRole(),
                        user.isVerified(),
                        user.getCompanyName(),
                        user.isEnabled(),
                        user.getBranch(),
                        user.getSemester()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(userDtos);
    }

    @PostMapping("/users")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> createUser(@RequestBody Users user) {
        try {
            // Basic validation
            if (userRepo.findByUsername(user.getUsername()).isPresent()) {
                return ResponseEntity.badRequest().body("Username already exists");
            }
            if (userRepo.findByEmail(user.getEmail()).isPresent()) {
                return ResponseEntity.badRequest().body("Email already exists");
            }

            String rawPassword = user.getPassword(); // Capture raw password for email

            // Encrypt password if provided (simplistic, better to force it)
            if (user.getPassword() != null && !user.getPassword().isEmpty()) {
                user.setPassword(passwordEncoder.encode(user.getPassword()));
            }

            // AUTO-VERIFY: Any user created by an Admin should be verified by default
            user.setVerified(true);

            Users savedUser = userRepo.save(user);

            // Send Welcome Email
            try {
                if (rawPassword != null && !rawPassword.isEmpty()) {
                    emailService.sendAccountCreatedEmail(savedUser.getEmail(), savedUser.getUsername(),
                            savedUser.getRole(),
                            rawPassword);
                }
            } catch (Exception e) {
                // Log but don't fail the request
                System.err.println("Failed to send welcome email: " + e.getMessage());
            }

            return ResponseEntity.ok(new UserDto(
                    savedUser.getId(),
                    savedUser.getUsername(),
                    savedUser.getEmail(),
                    savedUser.getRole(),
                    savedUser.isVerified(),
                    savedUser.getCompanyName(),
                    savedUser.isEnabled(),
                    savedUser.getBranch(),
                    savedUser.getSemester()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to create user: " + e.getMessage());
        }
    }

    @PutMapping("/users/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> updateUser(@PathVariable Integer id, @RequestBody Users updatedUser) {
        return userRepo.findById(id)
                .map(user -> {
                    user.setUsername(updatedUser.getUsername());
                    user.setEmail(updatedUser.getEmail());
                    user.setRole(updatedUser.getRole());

                    // Fix for accidental lockout: If becoming Admin/SuperAdmin, ensure verified is
                    // true
                    if ("ADMIN".equals(updatedUser.getRole()) || "SUPER_ADMIN".equals(updatedUser.getRole())
                            || "COMPANY_ADMIN".equals(updatedUser.getRole())) {
                        user.setVerified(true);
                    } else {
                        user.setVerified(updatedUser.isVerified());
                    }

                    user.setCompanyName(updatedUser.getCompanyName()); // Update company

                    // Only update password if new one is provided
                    if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
                        user.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
                    }

                    Users saved = userRepo.save(user);
                    return ResponseEntity.ok(new UserDto(
                            saved.getId(),
                            saved.getUsername(),
                            saved.getEmail(),
                            saved.getRole(),
                            saved.isVerified(),
                            saved.getCompanyName(),
                            saved.isEnabled(),
                            saved.getBranch(),
                            saved.getSemester()));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Integer id) {
        if (userRepo.existsById(id)) {
            userRepo.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    // Toggle company enabled/disabled status (Super Admin only)
    @PutMapping("/users/{id}/toggle-status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> toggleCompanyStatus(@PathVariable Integer id) {
        return userRepo.findById(id)
                .map(user -> {
                    // Toggle the enabled status
                    user.setEnabled(!user.isEnabled());
                    Users saved = userRepo.save(user);

                    return ResponseEntity.ok(new java.util.HashMap<String, Object>() {
                        {
                            put("id", saved.getId());
                            put("username", saved.getUsername());
                            put("companyName", saved.getCompanyName());
                            put("enabled", saved.isEnabled());
                            put("message", saved.isEnabled() ? "Company enabled successfully"
                                    : "Company disabled successfully");
                        }
                    });
                })
                .orElse(ResponseEntity.notFound().build());
    }
}