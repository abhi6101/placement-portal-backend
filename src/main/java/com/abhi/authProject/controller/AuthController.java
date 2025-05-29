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

import java.util.Map;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "https://hack-2-hired.onrender.com") // Adjust this based on your frontend's actual origin
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JWTService jwtService; // You might not directly use this for forgotten passwords, but keep it if used elsewhere.

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            // The verifyAndLogin method likely handles email verification and throws IllegalStateException
            String token = userService.verifyAndLogin(
                loginRequest.getUsername(),
                loginRequest.getPassword()
            );

            // If verifyAndLogin succeeds, proceed with Spring Security authentication
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getUsername(),
                    loginRequest.getPassword()
                )
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "username", authentication.getName(),
                    "roles", authentication.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.toList())
            ));
        } catch (BadCredentialsException e) {
            // This catches both incorrect username/password AND unverified email (if verifyAndLogin throws BadCredentialsException for unverified)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid username or password."));
        } catch (IllegalStateException e) {
            // This specifically catches the "Please verify your email address" message from UserService
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            // Catch any other unexpected errors
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "An error occurred during login: " + e.getMessage()));
        }
    }

    // --- REGISTER ENDPOINT ---
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest registerRequest) {
        try {
            Users newUser = new Users();
            newUser.setUsername(registerRequest.getUsername());
            newUser.setEmail(registerRequest.getEmail());
            newUser.setPassword(registerRequest.getPassword());
            newUser.setRole(registerRequest.getRole());

            userService.registerUser(newUser); // This should handle sending the OTP email
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Registration successful! A verification code has been sent to your email. Please check your inbox and enter the code on the verification page."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Registration failed: " + e.getMessage()));
        }
    }

    // --- EMAIL VERIFICATION CODE ENDPOINT ---
    @PostMapping("/verify-code")
    public ResponseEntity<?> verifyCode(@RequestBody VerificationCodeRequest request) {
        try {
            boolean verified = userService.verifyAccountWithCode(request.getIdentifier(), request.getCode());
            if (verified) {
                return ResponseEntity.ok(Map.of("message", "Account successfully verified! You can now log in."));
            } else {
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid or expired verification code. Please try again or re-register if the code has expired."));
            }
        } catch (Exception e) {
            System.err.println("Error verifying account with code: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "An error occurred during verification: " + e.getMessage()));
        }
    }

    // --- NEW: FORGOT PASSWORD ENDPOINT ---
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        try {
            // This method in your UserService will handle token generation and email sending
            userService.initiatePasswordReset(request.getEmailOrUsername());
            // It's good practice to send a generic success message to prevent email enumeration attacks
            return ResponseEntity.ok(Map.of("message", "If an account with the provided email or username exists, a password reset link has been sent. Please check your inbox."));
        } catch (IllegalArgumentException e) {
            // This might catch cases where the email/username is not found,
            // but for security, you might still return the generic success message.
            // For now, returning specific message, but consider security implications.
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            System.err.println("Error initiating password reset: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "An error occurred while initiating password reset. Please try again later."));
        }
    }

    // --- NEW: RESET PASSWORD ENDPOINT ---
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            // This method in your UserService will handle token validation and password update
            userService.resetPassword(request.getToken(), request.getNewPassword());
            return ResponseEntity.ok(Map.of("message", "Your password has been successfully reset. You can now log in with your new password."));
        } catch (IllegalArgumentException e) {
            // Catches invalid/expired token or password policy violations
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            System.err.println("Error resetting password: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "An error occurred while resetting your password. Please try again."));
        }
    }


    // --- DTOs for Request Bodies ---

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    static class LoginRequest {
        private String username;
        private String password;
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

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    static class VerificationCodeRequest {
        private String identifier; // Can be username or email
        private String code;
    }

    // --- NEW DTO for Forgot Password Request ---
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    static class ForgotPasswordRequest {
        private String emailOrUsername; // Field to accept either email or username
    }

    // --- NEW DTO for Reset Password Request ---
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    static class ResetPasswordRequest {
        private String token;
        private String newPassword;
    }
}