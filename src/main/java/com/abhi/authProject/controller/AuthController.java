package com.abhi.authProject.controller;

import com.abhi.authProject.Jwt.JWTService;
import com.abhi.authProject.model.Users; // Assuming your Users model is here
import com.abhi.authProject.service.UserService; // Changed to UserService
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

import java.net.URI; // Import for URI (if you decide to redirect)
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "https://hack-2-hired.onrender.com") // Adjust this based on your frontend's actual origin
@RequestMapping("/api/auth") // Added a base mapping for auth endpoints
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JWTService jwtService;

    @Autowired
    private UserService userService; // Changed to UserService

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            // Call the modified UserService login method
            String token = userService.verifyAndLogin(
                loginRequest.getUsername(),
                loginRequest.getPassword()
            );

            // If verifyAndLogin succeeds, manually set authentication for SecurityContextHolder
            // This is needed because verifyAndLogin might not automatically set it.
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
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid username or password"));
        } catch (IllegalStateException e) { // Catch the specific exception for unverified users
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage())); // Will send "Please verify your email address to log in."
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "An error occurred during login: " + e.getMessage()));
        }
    }

    // --- NEW REGISTRATION ENDPOINT ---
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest registerRequest) {
        try {
            Users newUser = new Users();
            newUser.setUsername(registerRequest.getUsername());
            newUser.setEmail(registerRequest.getEmail());
            newUser.setPassword(registerRequest.getPassword());
            newUser.setRole(registerRequest.getRole());

            userService.registerUser(newUser); // Call UserService to handle registration logic
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Registration successful! Please check your email to verify your account."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Registration failed: " + e.getMessage()));
        }
    }

    // --- NEW EMAIL VERIFICATION ENDPOINT ---
    @GetMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(@RequestParam("token") String token) {
        try {
            boolean verified = userService.verifyEmail(token);
            if (verified) {
                // Redirect to a frontend success page after successful verification
                // IMPORTANT: Adjust this URL to your actual frontend's "Email Verified" page
                // Or you can redirect directly to login.html if you prefer.
                return ResponseEntity.status(HttpStatus.FOUND)
                                 .location(URI.create("https://hack-2-hired.onrender.com/login.html?verified=true")) // Adjust URL
                                 .build();
            } else {
                // Redirect to a frontend error page for invalid/expired token
                return ResponseEntity.status(HttpStatus.FOUND)
                                 .location(URI.create("https://hack-2-hired.onrender.com/error.html?reason=invalid_token")) // Adjust URL
                                 .build();
            }
        } catch (Exception e) {
            System.err.println("Error verifying email: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.FOUND)
                             .location(URI.create("https://hack-2-hired.onrender.com/error.html?reason=server_error")) // Adjust URL
                             .build();
        }
    }


    // --- DTOs for Request Bodies ---

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor // Add @AllArgsConstructor for convenience if needed
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
}