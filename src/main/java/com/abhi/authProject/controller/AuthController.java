package com.abhi.authProject.controller;

import com.abhi.authProject.Jwt.JWTService;
import com.abhi.authProject.model.Users;
import com.abhi.authProject.service.UserService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.abhi.authProject.model.PasswordResetToken;
import com.abhi.authProject.repo.PasswordResetTokenRepo;
import com.abhi.authProject.service.EmailService;
import jakarta.transaction.Transactional;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "https://hack-2-hired.onrender.com") // Adjust this based on your frontend's actual origin
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JWTService jwtService;

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordResetTokenRepo passwordResetTokenRepo;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            String token = userService.verifyAndLogin(
                    loginRequest.getUsername(),
                    loginRequest.getPassword());

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "username", authentication.getName(),
                    "roles", authentication.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.toList())));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid username or password"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "An error occurred during login: " + e.getMessage()));
        }
    }

    // --- REGISTER ENDPOINT (MODIFIED RESPONSE) ---
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest registerRequest) {
        try {
            Users newUser = new Users();
            newUser.setUsername(registerRequest.getUsername());
            newUser.setEmail(registerRequest.getEmail());
            newUser.setPassword(registerRequest.getPassword());
            newUser.setRole(registerRequest.getRole());

            userService.registerUser(newUser); // This will send the OTP email
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message",
                    "Registration successful! A verification code has been sent to your email. Please check your inbox and enter the code on the verification page."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Registration failed: " + e.getMessage()));
        }
    }

    // --- NEW EMAIL VERIFICATION CODE ENDPOINT ---
    @PostMapping("/verify-code")
    public ResponseEntity<?> verifyCode(@RequestBody VerificationCodeRequest request) {
        try {
            boolean verified = userService.verifyAccountWithCode(request.getIdentifier(), request.getCode());
            if (verified) {
                return ResponseEntity.ok(Map.of("message", "Account successfully verified! You can now log in."));
            } else {
                return ResponseEntity.badRequest().body(Map.of("message",
                        "Invalid or expired verification code. Please try again or re-register if the code has expired."));
            }
        } catch (Exception e) {
            System.err.println("Error verifying account with code: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "An error occurred during verification: " + e.getMessage()));
        }
    }

    // Removed the old /verify-email GET endpoint for link-based verification

    // --- DTOs for Request Bodies ---

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    static class LoginRequest {
        private String username;
        private String password;
    }

    // Forgot Password - Request reset
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        try {
            Users user = userService.findByEmail(email);
            if (user == null) {
                // Don't reveal if email exists (security)
                return ResponseEntity.ok(Map.of("message", "If the email exists, a reset link has been sent."));
            }

            // Delete any existing tokens for this user
            passwordResetTokenRepo.findByUser(user).ifPresent(passwordResetTokenRepo::delete);

            // Generate new token
            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = new PasswordResetToken(token, user);
            passwordResetTokenRepo.save(resetToken);

            // Send email
            try {
                emailService.sendPasswordResetEmail(user.getEmail(), token);
            } catch (Exception e) {
                // Log error but don't reveal to user
                System.err.println("Failed to send reset email: " + e.getMessage());
            }

            return ResponseEntity.ok(Map.of("message", "If the email exists, a reset link has been sent."));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("message", "If the email exists, a reset link has been sent."));
        }
    }

    // Validate reset token
    @PostMapping("/validate-token")
    public ResponseEntity<?> validateToken(@RequestBody Map<String, String> request) {
        String token = request.get("token");

        PasswordResetToken resetToken = passwordResetTokenRepo.findByToken(token).orElse(null);

        if (resetToken == null || resetToken.isExpired()) {
            return ResponseEntity.badRequest().body(Map.of("valid", false, "message", "Invalid or expired token"));
        }

        return ResponseEntity.ok(Map.of("valid", true));
    }

    // Reset password with token
    @PostMapping("/reset-password")
    @Transactional
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String newPassword = request.get("newPassword");

        PasswordResetToken resetToken = passwordResetTokenRepo.findByToken(token).orElse(null);

        if (resetToken == null || resetToken.isExpired()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid or expired token"));
        }

        // Update password
        Users user = resetToken.getUser();
        userService.updatePassword(user, newPassword);

        // Delete the token (one-time use)
        passwordResetTokenRepo.delete(resetToken);

        // Send confirmation email
        try {
            emailService.sendPasswordResetConfirmation(user.getEmail());
        } catch (Exception e) {
            // Log error but don't fail the password reset
            System.err.println("Failed to send confirmation email: " + e.getMessage());
        }

        return ResponseEntity.ok(Map.of("message", "Password reset successful"));
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    static class RegisterRequest {
        private String username;
        private String email;
        private String password;
        private String role;
    }

    // NEW DTO for verification code request
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    static class VerificationCodeRequest {
        private String identifier; // Can be username or email
        private String code;
    }
}