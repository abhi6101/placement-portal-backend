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

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getCurrentUser(org.springframework.security.core.Authentication auth) {
        String username = auth.getName();
        Users user = userRepo.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));

        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("id", user.getId());
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("role", user.getRole());
        response.put("name", user.getName() != null ? user.getName() : "");
        response.put("phone", user.getPhone() != null ? user.getPhone() : "");
        response.put("branch", user.getBranch() != null ? user.getBranch() : "");
        response.put("semester", user.getSemester() != null ? user.getSemester() : 0);
        response.put("batch", user.getBatch() != null ? user.getBatch() : "");
        response.put("computerCode", user.getComputerCode() != null ? user.getComputerCode() : "");
        response.put("companyName", user.getCompanyName() != null ? user.getCompanyName() : "");

        return ResponseEntity.ok(response);
    }

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
            String username = jwtUtil.extractUsername(token);
            Users user = userService.findByUsername(username);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "User not found"));
            }
            return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "role", user.getRole(),
                "branch", user.getBranch() != null ? user.getBranch() : "",
                "semester", user.getSemester() != null ? user.getSemester() : 0,
                "batch", user.getBatch() != null ? user.getBatch() : "",
                "computerCode", user.getComputerCode() != null ? user.getComputerCode() : "",
                "aadharNumber", user.getAadharNumber() != null ? user.getAadharNumber() : "",
                "fullName", user.getFullName() != null ? user.getFullName() : "",
                "fatherName", user.getFatherName() != null ? user.getFatherName() : "",
                "institution", user.getInstitution() != null ? user.getInstitution() : "",
                "session", user.getSession() != null ? user.getSession() : "",
                "mobilePrimary", user.getMobilePrimary() != null ? user.getMobilePrimary() : "",
                "mobileSecondary", user.getMobileSecondary() != null ? user.getMobileSecondary() : "",
                "enrollmentNumber", user.getEnrollmentNumber() != null ? user.getEnrollmentNumber() : "",
                "startYear", user.getStartYear() != null ? user.getStartYear() : "",
                "companyName", user.getCompanyName() != null ? user.getCompanyName() : ""
            ));
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

    // Update Profile Endpoint
    @PutMapping("/update-profile")
    public ResponseEntity<?> updateProfile(@RequestBody UpdateProfileRequest request,
            @RequestHeader("Authorization") String authHeader) {
        try {
            // Extract token from Authorization header
            String token = authHeader.replace("Bearer ", "");
            String username = jwtUtil.extractUsername(token);

            // Find user by username
            Users user = userService.findByUsername(username);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "User not found"));
            }

            // Update only the fields that are provided
            if (request.getFullName() != null)
                user.setFullName(request.getFullName());
            if (request.getFatherName() != null)
                user.setFatherName(request.getFatherName());
            if (request.getInstitution() != null)
                user.setInstitution(request.getInstitution());
            if (request.getAadharNumber() != null)
                user.setAadharNumber(request.getAadharNumber());
            if (request.getMobilePrimary() != null)
                user.setMobilePrimary(request.getMobilePrimary());
            if (request.getMobileSecondary() != null)
                user.setMobileSecondary(request.getMobileSecondary());
            if (request.getEnrollmentNumber() != null)
                user.setEnrollmentNumber(request.getEnrollmentNumber());
            if (request.getStartYear() != null)
                user.setStartYear(request.getStartYear());

            // Save updated user
            userService.updateUser(user);

            return ResponseEntity.ok(Map.of("message", "Profile updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to update profile: " + e.getMessage()));
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

        // Update name and phone if provided
        if (payload.containsKey("name")) {
            student.setName((String) payload.get("name"));
        }
        if (payload.containsKey("phone")) {
            student.setPhone((String) payload.get("phone"));
        }

        String branch = (String) payload.get("branch");
        Integer semester = payload.get("semester") != null ? Integer.parseInt(payload.get("semester").toString())
                : null;
        String batch = (String) payload.get("batch");

        // Update student profile
        if (branch != null)
            student.setBranch(branch);
        if (semester != null)
            student.setSemester(semester);
        if (batch != null)
            student.setBatch(batch);

        // Allow updating computer code if not already set (or if we want to allow
        // updates)
        if (payload.containsKey("computerCode")) {
            String computerCode = (String) payload.get("computerCode");
            // Check uniqueness if necessary, or let DB throw error
            student.setComputerCode(computerCode);
        }

        student.setLastProfileUpdate(java.time.LocalDate.now());

        Users saved = userRepo.save(student);

        return ResponseEntity.ok(Map.of(
                "message", "Profile updated successfully",
                "name", saved.getName() != null ? saved.getName() : "",
                "phone", saved.getPhone() != null ? saved.getPhone() : "",
                "branch", saved.getBranch() != null ? saved.getBranch() : "",
                "semester", saved.getSemester() != null ? saved.getSemester() : 0));
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

