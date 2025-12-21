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

    // Link-based /me endpoint removed. Please use the Header-based one below.

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

            if (user != null) {
                user.setLastLoginDate(java.time.LocalDateTime.now());
                userRepo.save(user);
            }

            String companyName = (user != null) ? user.getCompanyName() : null;

            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "username", authentication.getName(),
                    "companyName", companyName != null ? companyName : "",
                    "branch", (user != null && user.getBranch() != null) ? user.getBranch() : "",
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

    // Get current user information
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String username = jwtService.extractUserName(token);
            Users user = userRepo.findByUsername(username).orElse(null);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "User not found"));
            }

            // Use HashMap for more than 10 key-value pairs
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("id", user.getId());
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            response.put("role", user.getRole());
            response.put("branch", user.getBranch() != null ? user.getBranch() : "");
            response.put("semester", user.getSemester() != null ? user.getSemester() : 0);
            response.put("batch", user.getBatch() != null ? user.getBatch() : "");
            response.put("computerCode", user.getComputerCode() != null ? user.getComputerCode() : "");
            response.put("aadharNumber", user.getAadharNumber() != null ? user.getAadharNumber() : "");
            response.put("fullName", user.getFullName() != null ? user.getFullName() : "");
            response.put("fatherName", user.getFatherName() != null ? user.getFatherName() : "");
            response.put("institution", user.getInstitution() != null ? user.getInstitution() : "");
            response.put("session", user.getSession() != null ? user.getSession() : "");
            response.put("mobilePrimary", user.getMobilePrimary() != null ? user.getMobilePrimary() : "");
            response.put("mobileSecondary", user.getMobileSecondary() != null ? user.getMobileSecondary() : "");
            response.put("enrollmentNumber", user.getEnrollmentNumber() != null ? user.getEnrollmentNumber() : "");
            response.put("startYear", user.getStartYear() != null ? user.getStartYear() : "");
            response.put("companyName", user.getCompanyName() != null ? user.getCompanyName() : "");

            // Return Images
            response.put("idCardImage", user.getIdCardImage() != null ? user.getIdCardImage() : "");
            response.put("aadharCardImage", user.getAadharCardImage() != null ? user.getAadharCardImage() : "");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to get user info: " + e.getMessage()));
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
                String batch = registerRequest.getBatch();

                if (branch == null) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("message", "Branch is required"));
                }
                if (semester == null) {
                    return ResponseEntity.badRequest().body(Map.of("message", "Semester is required for students"));
                }

                newUser.setBranch(branch);
                newUser.setSemester(semester);
                newUser.setBatch(batch);
                newUser.setComputerCode(registerRequest.getComputerCode());

                // Save verified identity data from ID card scan
                newUser.setFullName(registerRequest.getFullName());
                newUser.setFatherName(registerRequest.getFatherName());
                newUser.setInstitution(registerRequest.getInstitution());
                newUser.setSession(registerRequest.getSession());

                // Save mobile numbers
                newUser.setMobilePrimary(registerRequest.getMobilePrimary());
                newUser.setMobileSecondary(registerRequest.getMobileSecondary());

                // Save additional academic info
                newUser.setEnrollmentNumber(registerRequest.getEnrollmentNumber());
                newUser.setStartYear(registerRequest.getStartYear());

                // SECURITY CRITICAL: Duplicate Aadhar Check
                if (registerRequest.getAadharNumber() != null) {
                    if (userRepo.findByAadharNumber(registerRequest.getAadharNumber()).isPresent()) {
                        return ResponseEntity.badRequest()
                                .body(Map.of("message", "Aadhar Number is already registered with another account."));
                    }
                    newUser.setAadharNumber(registerRequest.getAadharNumber());
                }

                // Save Images
                newUser.setIdCardImage(registerRequest.getIdCardImage());
                newUser.setAadharCardImage(registerRequest.getAadharCardImage());

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

        // Update basic fields
        if (payload.containsKey("name"))
            student.setName((String) payload.get("name"));
        if (payload.containsKey("phone"))
            student.setPhone((String) payload.get("phone"));

        // Update new ID/Registration fields
        if (payload.containsKey("fullName"))
            student.setFullName((String) payload.get("fullName"));
        if (payload.containsKey("fatherName"))
            student.setFatherName((String) payload.get("fatherName"));
        if (payload.containsKey("institution"))
            student.setInstitution((String) payload.get("institution"));
        // Aadhar is usually not editable after registration for security, but we skip
        // it here

        if (payload.containsKey("mobilePrimary"))
            student.setMobilePrimary((String) payload.get("mobilePrimary"));
        if (payload.containsKey("mobileSecondary"))
            student.setMobileSecondary((String) payload.get("mobileSecondary"));
        if (payload.containsKey("enrollmentNumber"))
            student.setEnrollmentNumber((String) payload.get("enrollmentNumber"));
        if (payload.containsKey("startYear"))
            student.setStartYear((String) payload.get("startYear"));
        if (payload.containsKey("session"))
            student.setSession((String) payload.get("session"));

        String branch = (String) payload.get("branch");
        // Handle semester safely as it might be Integer or String
        Integer semester = null;
        if (payload.get("semester") != null) {
            try {
                semester = Integer.parseInt(payload.get("semester").toString());
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        String batch = (String) payload.get("batch");

        // Update student profile
        if (branch != null)
            student.setBranch(branch);
        if (semester != null)
            student.setSemester(semester);
        if (batch != null)
            student.setBatch(batch);

        if (payload.containsKey("computerCode")) {
            student.setComputerCode((String) payload.get("computerCode"));
        }

        student.setLastProfileUpdate(java.time.LocalDate.now());

        Users saved = userRepo.save(student);

        // Use HashMap for response map to avoid 10-item limit
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("message", "Profile updated successfully");
        response.put("name", saved.getName() != null ? saved.getName() : "");
        response.put("fullName", saved.getFullName() != null ? saved.getFullName() : "");
        response.put("phone", saved.getPhone() != null ? saved.getPhone() : "");
        response.put("mobilePrimary", saved.getMobilePrimary() != null ? saved.getMobilePrimary() : "");
        response.put("mobileSecondary", saved.getMobileSecondary() != null ? saved.getMobileSecondary() : "");
        response.put("branch", saved.getBranch() != null ? saved.getBranch() : "");
        response.put("semester", saved.getSemester() != null ? saved.getSemester() : 0);
        response.put("enrollmentNumber", saved.getEnrollmentNumber() != null ? saved.getEnrollmentNumber() : "");
        response.put("startYear", saved.getStartYear() != null ? saved.getStartYear() : "");
        response.put("session", saved.getSession() != null ? saved.getSession() : "");

        return ResponseEntity.ok(response);
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

        // Check if any required field is null
        if (student.getName() == null || student.getPhone() == null ||
                student.getBranch() == null || student.getSemester() == null) {
            needsUpdate = true;
            reason = "Complete your profile to access all features";
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
                "currentName", student.getName() != null ? student.getName() : "",
                "currentPhone", student.getPhone() != null ? student.getPhone() : "",
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
        private String batch; // e.g. 2022-2027
        private String computerCode;
        private String aadharNumber;

        // Verified Identity Data (from ID card scan)
        private String fullName;
        private String fatherName;
        private String institution;
        private String session;

        // Mobile Numbers
        private String mobilePrimary;
        private String mobileSecondary;

        // Additional Academic Info
        private String enrollmentNumber;
        private String startYear;

        // Images
        private String idCardImage;
        private String aadharCardImage;
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
