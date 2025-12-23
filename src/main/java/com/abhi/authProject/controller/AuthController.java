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
            // Support both username and computerCode
            String identifier = loginRequest.getComputerCode() != null && !loginRequest.getComputerCode().isEmpty()
                    ? loginRequest.getComputerCode()
                    : loginRequest.getUsername();

            String token = userService.verifyAndLogin(identifier, loginRequest.getPassword());

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(identifier, loginRequest.getPassword()));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Find user by computerCode or username
            Users user = userRepo.findByComputerCodeOrUsername(identifier).orElse(null);

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
                    .body(Map.of("message", "Invalid credentials"));
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
            response.put("address", user.getAddress() != null ? user.getAddress() : "");

            // Return Images
            response.put("idCardImage", user.getIdCardImage() != null ? user.getIdCardImage() : "");
            response.put("aadharCardImage", user.getAadharCardImage() != null ? user.getAadharCardImage() : "");
            response.put("profilePictureUrl", user.getProfilePictureUrl() != null ? user.getProfilePictureUrl() : "");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to get user info: " + e.getMessage()));
        }
    }

    // --- REGISTER ENDPOINT (MODIFIED WITH LEGACY MIGRATION) ---
    // --- REGISTER ENDPOINT (MODIFIED WITH LEGACY MIGRATION) ---
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest registerRequest,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // Check if user already exists by email
            java.util.Optional<Users> existingUserOpt = userRepo.findByEmail(registerRequest.getEmail());

            if (existingUserOpt.isPresent()) {
                Users existingUser = existingUserOpt.get();

                // Check if this is an "Old User" (No Computer Code/Incomplete Profile)
                boolean isLegacyUser = existingUser.getComputerCode() == null
                        || existingUser.getComputerCode().isEmpty();

                if (isLegacyUser) {
                    // --- SECURITY CHECK: Require Authorization Token for Updates ---
                    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(Map.of("message",
                                        "Account exists. Please use 'Forgot Password' to recover your account securely."));
                    }

                    try {
                        String token = authHeader.replace("Bearer ", "");
                        String username = jwtService.extractUserName(token);

                        // We must ensure the token belongs to the USER strictly (by username or email)
                        // existingUser might have a random username, but the token was generated for
                        // it.
                        if (!existingUser.getUsername().equals(username)) {
                            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                    .body(Map.of("message", "Security validation failed. Token mismatch."));
                        }
                    } catch (Exception e) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(Map.of("message", "Security validation failed. Invalid token."));
                    }

                    // --- MIGRATION LOGIC: Update Existing Account ---

                    // Update basic fields
                    // Note: We don't update username yet to computerCode until we are sure,
                    // or maybe we should? The register request sends 'username' but usually we want
                    // computerCode.

                    // If the user entered a password in the form, update it.
                    if (registerRequest.getPassword() != null && !registerRequest.getPassword().isEmpty()) {
                        userService.updatePassword(existingUser, registerRequest.getPassword());
                    }

                    // Update Role
                    existingUser.setRole(registerRequest.getRole());

                    // Update Student Details
                    if ("USER".equals(registerRequest.getRole())) {
                        existingUser.setBranch(registerRequest.getBranch());
                        existingUser.setSemester(registerRequest.getSemester());
                        existingUser.setBatch(registerRequest.getBatch());
                        existingUser.setComputerCode(registerRequest.getComputerCode());

                        // Identity Data
                        existingUser.setFullName(registerRequest.getFullName());
                        existingUser.setFatherName(registerRequest.getFatherName());
                        existingUser.setInstitution(registerRequest.getInstitution());
                        existingUser.setSession(registerRequest.getSession());

                        // Mobile
                        existingUser.setMobilePrimary(registerRequest.getMobilePrimary());
                        existingUser.setMobileSecondary(registerRequest.getMobileSecondary());

                        // Academic
                        existingUser.setEnrollmentNumber(registerRequest.getEnrollmentNumber());
                        existingUser.setStartYear(registerRequest.getStartYear());

                        // Aadhar Check (Ensure no one else used this Aadhar)
                        if (registerRequest.getAadharNumber() != null) {
                            java.util.Optional<Users> aadharUser = userRepo
                                    .findByAadharNumber(registerRequest.getAadharNumber());
                            if (aadharUser.isPresent() && aadharUser.get().getId() != existingUser.getId()) {
                                return ResponseEntity.badRequest().body(
                                        Map.of("message", "Aadhar Number is already registered with another account."));
                            }
                            existingUser.setAadharNumber(registerRequest.getAadharNumber());
                            existingUser.setAddress(registerRequest.getAddress());
                        }

                        // Images
                        existingUser.setIdCardImage(registerRequest.getIdCardImage());
                        existingUser.setAadharCardImage(registerRequest.getAadharCardImage());
                        existingUser.setProfilePictureUrl(registerRequest.getProfilePictureUrl());

                        existingUser.setLastProfileUpdate(java.time.LocalDate.now());
                    }

                    // Change username to computerCode for consistency?
                    // Usually legacy users have a random username.
                    // New system prefers computerCode as username or unique identifier.
                    if (registerRequest.getComputerCode() != null && !registerRequest.getComputerCode().isEmpty()) {
                        existingUser.setUsername(registerRequest.getComputerCode());
                    }

                    // Mark verified (Since they likely came from Forgot Password flow)
                    // Or we can assume if they are updating, they are verified.
                    existingUser.setVerified(true);

                    userRepo.save(existingUser);

                    return ResponseEntity.ok(Map.of("message", "Account updated successfully! You can now log in."));

                } else {
                    // User exists and is NOT legacy (Duplicate Account)
                    return ResponseEntity.badRequest()
                            .body(Map.of("message", "Email already registered. Please log in."));
                }
            }

            // --- STANDARD NEW USER REGISTRATION ---
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
                    newUser.setAddress(registerRequest.getAddress());
                }

                // Save Images
                newUser.setIdCardImage(registerRequest.getIdCardImage());
                newUser.setAadharCardImage(registerRequest.getAadharCardImage());
                newUser.setProfilePictureUrl(registerRequest.getProfilePictureUrl());

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
        private String computerCode; // Support Computer Code login
        private String password;
    }

    // Forgot Password - Request reset
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        try {
            Users user = userService.findByEmail(email);
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Email address not found. Please register."));
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
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "Failed to send email. Please try again."));
            }

            return ResponseEntity.ok(Map.of("message", "OTP sent successfully!"));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("message", "If the email exists, an OTP has been sent."));
        }
    }

    // Verify OTP with Smart Routing
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String otp = request.get("otp");

        Users user = userService.findByEmail(email);
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Invalid email or OTP"));
        }

        PasswordResetToken resetToken = passwordResetTokenRepo.findByUser(user).orElse(null);

        if (resetToken == null || !resetToken.getToken().equals(otp) || resetToken.isExpired()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Invalid or expired OTP"));
        }

        // Check if user has complete data
        boolean hasComputerCode = user.getComputerCode() != null && !user.getComputerCode().isEmpty();
        boolean hasAadhar = user.getAadharNumber() != null && !user.getAadharNumber().isEmpty();
        boolean hasDob = user.getDob() != null;
        boolean hasGender = user.getGender() != null && !user.getGender().isEmpty();

        boolean isComplete = hasComputerCode && hasAadhar && hasDob && hasGender;

        // EXCEPTION: Admins do NOT need to complete registration (No ID/Aadhar needed)
        // If user is ANY kind of Admin, treat as complete.
        if (user.getRole() != null && user.getRole().toUpperCase().contains("ADMIN")) {
            isComplete = true;
        }

        // Generate recovery token (JWT valid for 1 hour)
        String recoveryToken = jwtService.generateToken(user.getUsername());

        if (isComplete) {
            // Route A: Simple password reset
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "route", "SIMPLE_RESET",
                    "token", recoveryToken,
                    "userData", Map.of(
                            "name", user.getFullName() != null ? user.getFullName() : user.getUsername(),
                            "computerCode", user.getComputerCode(),
                            "email", user.getEmail(),
                            "role", user.getRole())));
        } else {
            // Route B: Full verification needed
            Map<String, Object> existingData = new java.util.HashMap<>();
            if (user.getFullName() != null)
                existingData.put("name", user.getFullName());
            if (user.getUsername() != null)
                existingData.put("username", user.getUsername());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "route", "FULL_VERIFICATION",
                    "token", recoveryToken,
                    "userData", Map.of(
                            "email", user.getEmail(),
                            "existingData", existingData)));
        }
    }

    // Reset password with Token (Route A - Simple Reset)
    @PostMapping("/reset-password")
    @Transactional
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            String email = request.get("email");
            String newPassword = request.get("newPassword");

            System.out.println("🔐 Password Reset Request - Email: " + email);
            System.out.println("Authorization Header Present: " + (authHeader != null));

            // Verify token from header
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                System.err.println("❌ Missing or invalid authorization header");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Missing or invalid authorization token"));
            }

            String token = authHeader.replace("Bearer ", "");
            System.out.println("Token extracted, length: " + token.length());

            String username;
            try {
                username = jwtService.extractUserName(token);
                System.out.println("✅ Username extracted from token: " + username);
            } catch (Exception e) {
                System.err.println("❌ Failed to extract username from token: " + e.getMessage());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Invalid or expired token"));
            }

            Users user = userService.findByEmail(email);
            if (user == null) {
                System.err.println("❌ User not found for email: " + email);
                return ResponseEntity.badRequest().body(Map.of("message", "User not found"));
            }

            if (!user.getUsername().equals(username)) {
                System.err.println("❌ Username mismatch - Token: " + username + ", User: " + user.getUsername());
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid request - username mismatch"));
            }

            System.out.println("✅ Validation passed, updating password...");

            // Update password
            userService.updatePassword(user, newPassword);

            // Delete any password reset tokens
            passwordResetTokenRepo.findByUser(user).ifPresent(passwordResetTokenRepo::delete);

            // Send confirmation email
            try {
                emailService.sendPasswordResetConfirmation(user.getEmail());
            } catch (Exception e) {
                System.err.println("Failed to send confirmation email: " + e.getMessage());
            }

            // Generate NEW Login Token for Auto-Login / Verification
            String newToken = jwtService.generateToken(user.getUsername());

            System.out.println("✅ Password reset successful for user: " + username);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Password reset successful",
                    "token", newToken));
        } catch (Exception e) {
            System.err.println("❌ Unexpected error in resetPassword: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "An error occurred: " + e.getMessage()));
        }
    }

    // Complete Recovery with Full Verification (Route B - Legacy User Migration)
    @PostMapping("/complete-recovery")
    @Transactional
    public ResponseEntity<?> completeRecovery(@RequestBody Map<String, Object> request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        // Verify token from header
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Missing or invalid authorization token"));
        }

        String token = authHeader.replace("Bearer ", "");
        String username = jwtService.extractUserName(token);

        String email = (String) request.get("email");
        Users user = userService.findByEmail(email);

        if (user == null || !user.getUsername().equals(username)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid request"));
        }

        // Extract data from request
        String computerCode = (String) request.get("computerCode");
        String aadharNumber = (String) request.get("aadharNumber");
        String dob = (String) request.get("dob");
        String gender = (String) request.get("gender");
        String name = (String) request.get("name");
        String newPassword = (String) request.get("newPassword");
        String semester = (String) request.get("semester");
        String enrollmentNumber = (String) request.get("enrollmentNumber");
        String selfieImage = (String) request.get("selfieImage");
        String idCardImage = (String) request.get("idCardImage");
        String aadharImage = (String) request.get("aadharImage");

        // Validate required fields
        if (computerCode == null || aadharNumber == null || dob == null || gender == null || newPassword == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Missing required fields"));
        }

        // Check if Computer Code already exists (for different user)
        if (userRepo.findByComputerCode(computerCode).isPresent()) {
            Users existingUser = userRepo.findByComputerCode(computerCode).get();
            if (existingUser.getId() != user.getId()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Computer Code is already in use"));
            }
        }

        // Check if Aadhar already exists (for different user)
        if (userRepo.findByAadharNumber(aadharNumber).isPresent()) {
            Users existingUser = userRepo.findByAadharNumber(aadharNumber).get();
            if (existingUser.getId() != user.getId()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Aadhar Number is already registered"));
            }
        }

        // Update user record - MIGRATE TO NEW SYSTEM
        user.setUsername(computerCode); // Change username to Computer Code
        user.setComputerCode(computerCode);
        user.setAadharNumber(aadharNumber);
        user.setDob(dob);
        user.setGender(gender);
        if (name != null)
            user.setFullName(name);
        if (semester != null) {
            try {
                user.setSemester(Integer.parseInt(semester));
            } catch (NumberFormatException e) {
                // Ignore invalid semester
            }
        }
        if (enrollmentNumber != null)
            user.setEnrollmentNumber(enrollmentNumber);

        // Save images
        if (idCardImage != null)
            user.setIdCardImage(idCardImage);
        if (aadharImage != null)
            user.setAadharCardImage(aadharImage);
        if (selfieImage != null)
            user.setProfilePictureUrl(selfieImage);

        // Mark as verified and no longer legacy
        user.setVerified(true);
        user.setLastProfileUpdate(java.time.LocalDate.now());

        // Update password
        userService.updatePassword(user, newPassword);

        // Save user
        userRepo.save(user);

        // Delete password reset token
        passwordResetTokenRepo.findByUser(user).ifPresent(passwordResetTokenRepo::delete);

        // Send confirmation email
        try {
            emailService.sendAccountUpgradeConfirmation(user.getEmail(), computerCode, name);
        } catch (Exception e) {
            System.err.println("Failed to send confirmation email: " + e.getMessage());
        }

        return ResponseEntity.ok(Map.of("success", true, "message", "Account recovery and upgrade successful"));
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
        private String address; // NEW: Address from Aadhar

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
        private String profilePictureUrl; // Added for selfie/avatar
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
