package com.abhi.authProject.controller;

import com.abhi.authProject.model.Users;
import com.abhi.authProject.repo.UserRepo;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/verification")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin
public class VerificationController {

    private final UserRepo userRepo;

    @PostMapping("/check-status")
    public ResponseEntity<?> checkVerificationStatus(@RequestBody VerificationStatusRequest request) {
        try {
            log.info("Checking verification status for computer code: {}", request.getComputerCode());

            if (request.getComputerCode() == null || request.getComputerCode().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Computer code is required"));
            }

            Optional<Users> existingUser = userRepo.findByComputerCode(request.getComputerCode());

            if (existingUser.isPresent()) {
                Users user = existingUser.get();
                log.info("User already registered with computer code: {}", request.getComputerCode());

                return ResponseEntity.ok(VerificationStatusResponse.builder()
                        .status("ALREADY_REGISTERED")
                        .message("User account already exists")
                        .userData(UserDataDTO.builder()
                                .username(user.getUsername())
                                .email(maskEmail(user.getEmail()))
                                .fullName(user.getName())
                                .build())
                        .build());
            }

            // In a real system, you might verify the device fingerprint against a "verified
            // devices" table here
            // For now, we assume if they aren't registered, they are a new user

            return ResponseEntity.ok(VerificationStatusResponse.builder()
                    .status("NEW_USER")
                    .message("User not registered. Proceed with verification.")
                    .build());

        } catch (Exception e) {
            log.error("Error checking verification status", e);
            return ResponseEntity.internalServerError().body(Map.of("message", "Error checking status"));
        }
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@"))
            return email;
        String[] parts = email.split("@");
        if (parts[0].length() <= 2)
            return email;
        return parts[0].substring(0, 2) + "****@" + parts[1];
    }

    @Data
    public static class VerificationStatusRequest {
        private String computerCode;
        private String deviceFingerprint;
        private String ipAddress;
        private Object location;
    }

    @Data
    @lombok.Builder
    public static class VerificationStatusResponse {
        private String status; // ALREADY_REGISTERED, NEW_USER, DEVICE_MISMATCH
        private String message;
        private UserDataDTO userData;
        // verification data fields if needed for resumption
        private Object data;
    }

    @Data
    @lombok.Builder
    public static class UserDataDTO {
        private String username;
        private String email;
        private String fullName;
    }
}
