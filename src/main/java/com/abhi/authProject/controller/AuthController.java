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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.abhi.authProject.model.PasswordResetToken;
import com.abhi.authProject.repo.PasswordResetTokenRepo;
import com.abhi.authProject.service.EmailService;
import jakarta.transaction.Transactional;

import java.util.Map;
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

    @Autowired
    private com.abhi.authProject.repo.UserRepo userRepo;

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

            Users user = userRepo.findByUsername(loginRequest.getUsername()).orElse(null);
            String companyName = (user != null) ? user.getCompanyName() : null;

            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "username", authentication.getName(),
                    "companyName", companyName != null ? companyName : "",
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

            // If registering as student (USER), validate and set branch/semester
            if ("USER".equals(registerRequest.getRole())) {
                String branch = registerRequest.getBranch();
                Integer semester = registerRequest.getSemester();

                // Validate branch
                if (branch == null || !java.util.Arrays.asList("IMCA", "MCA", "BCA").contains(branch)) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("message", "Invalid branch. Must be IMCA, MCA, or BCA"));
                }

                // Validate semester based on branch
                if (semester == null) {
                    return ResponseEntity.badRequest().body(Map.of("message", "Semester is required for students"));
                }
                if ("IMCA".equals(branch) && (semester < 1 || semester > 10)) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("message", "IMCA semester must be between 1 and 10"));
                }
                if ("MCA".equals(branch) && (semester < 1 || semester > 4)) {
                    return ResponseEntity.badRequest().body(Map.of("message", "MCA semester must be between 1 and 4"));
                }
                if ("BCA".equals(branch) && !java.util.Arrays.asList(2, 4, 6).contains(semester)) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("message", "BCA semester must be 2, 4, or 6 (representing Year 1, 2, or 3)"));
                }

                newUser.setBranch(branch);
                newUser.setSemester(semester);
                newUser.setLastProfileUpdate(java.time.LocalDate.now());
            }

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
                return ResponseEntity.ok(Map.of("message", "If the email exists, an OTP has been sent."));
            }

            // Delete any existing tokens for this user
            passwordResetTokenRepo.findByUser(user).ifPresent(passwordResetTokenRepo::delete);

            // Generate 6-digit OTP
            String otp = String.format("%06d", new java.util.Random().nextInt(1000000));
            PasswordResetToken resetToken = new PasswordResetToken(otp, user);
            passwordResetTokenRepo.save(resetToken);

            // Send email
            try {
                emailService.sendPasswordResetEmail(user.getEmail(), otp);
            } catch (Exception e) {
                // Log error but don't reveal to user
                System.err.println("Failed to send reset email: " + e.getMessage());
            }

            return ResponseEntity.ok(Map.of("message", "If the email exists, an OTP has been sent."));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("message", "If the email exists, an OTP has been sent."));
        }
    }

    // Verify OTP
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String otp = request.get("otp");

        Users user = userService.findByEmail(email);
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("valid", false, "message", "Invalid email or OTP"));
        }

        PasswordResetToken resetToken = passwordResetTokenRepo.findByUser(user).orElse(null);

        if (resetToken == null || !resetToken.getToken().equals(otp) || resetToken.isExpired()) {
            return ResponseEntity.badRequest().body(Map.of("valid", false, "message", "Invalid or expired OTP"));
        }

        return ResponseEntity.ok(Map.of("valid", true));
    }

    // Reset password with OTP
    @PostMapping("/reset-password")
    @Transactional
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String otp = request.get("otp");
        String newPassword = request.get("newPassword");

        Users user = userService.findByEmail(email);
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid email or OTP"));
        }

        PasswordResetToken resetToken = passwordResetTokenRepo.findByUser(user).orElse(null);

        if (resetToken == null || !resetToken.getToken().equals(otp) || resetToken.isExpired()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid or expired OTP"));
        }

        // Update password
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

    // --- STUDENT PROFILE UPDATE ENDPOINTS ---

    /**
     * Update student's branch and semester (Students only)
     */
    @PutMapping("/update-profile")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, Object> payload,
            org.springframework.security.core.Authentication auth) {
        String username = auth.getName();
        Users student = userRepo.findByUsername(username).orElse(null);

        if (student == null) {
            return ResponseEntity.notFound().build();
        }

        String branch = (String) payload.get("branch");
        Integer semester = Integer.parseInt(payload.get("semester").toString());

        // Validate branch
        if (!java.util.Arrays.asList("IMCA", "MCA", "BCA").contains(branch)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid branch. Must be IMCA, MCA, or BCA"));
        }

        // Validate semester based on branch
        if ("IMCA".equals(branch) && (semester < 1 || semester > 10)) {
            return ResponseEntity.badRequest().body(Map.of("message", "IMCA semester must be between 1 and 10"));
        }
        if ("MCA".equals(branch) && (semester < 1 || semester > 4)) {
            return ResponseEntity.badRequest().body(Map.of("message", "MCA semester must be between 1 and 4"));
        }
        if ("BCA".equals(branch) && !java.util.Arrays.asList(2, 4, 6).contains(semester)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "BCA semester must be 2, 4, or 6 (representing Year 1, 2, or 3)"));
        }

        // Update student profile
        student.setBranch(branch);
        student.setSemester(semester);
        student.setLastProfileUpdate(java.time.LocalDate.now());

        Users saved = userRepo.save(student);

        return ResponseEntity.ok(Map.of(
                "message", "Profile updated successfully",
                "branch", saved.getBranch(),
                "semester", saved.getSemester()));
    }

    /**
     * Check if student needs to update profile
     * Returns true if:
     * - Branch/semester is null OR
     * - Date > 2026-01-01 AND lastProfileUpdate < 2026-01-01
     */
    @GetMapping("/profile-status")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> profileStatus(org.springframework.security.core.Authentication auth) {
        String username = auth.getName();
        Users student = userRepo.findByUsername(username).orElse(null);

        if (student == null) {
            return ResponseEntity.notFound().build();
        }

        boolean needsUpdate = false;
        String reason = "";

        // Check if branch/semester is null
        if (student.getBranch() == null || student.getSemester() == null) {
            needsUpdate = true;
            reason = "Initial profile setup required";
        }
        // Check if date is after 2026-01-01 and last update was before
        else {
            java.time.LocalDate today = java.time.LocalDate.now();
            java.time.LocalDate cutoffDate = java.time.LocalDate.of(2026, 1, 1);

            if (today.isAfter(cutoffDate) || today.isEqual(cutoffDate)) {
                if (student.getLastProfileUpdate() == null || student.getLastProfileUpdate().isBefore(cutoffDate)) {
                    needsUpdate = true;
                    reason = "Semester update required for new academic year";
                }
            }
        }

        return ResponseEntity.ok(Map.of(
                "needsUpdate", needsUpdate,
                "reason", reason,
                "currentBranch", student.getBranch() != null ? student.getBranch() : "",
                "currentSemester", student.getSemester() != null ? student.getSemester() : 0));
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
        private String branch; // For students: IMCA, MCA, BCA
        private Integer semester; // For students: varies by branch
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