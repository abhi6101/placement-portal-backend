package com.abhi.authProject.controller;

import com.abhi.authProject.service.OtpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/otp")
@CrossOrigin(origins = "http://localhost:5173") // Allow frontend access
public class OtpController {

    @Autowired
    private OtpService otpService;

    @PostMapping("/send")
    public ResponseEntity<?> sendOtp(@RequestBody Map<String, String> request) {
        String phone = request.get("phone");
        if (phone == null || phone.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Phone number is required"));
        }

        // Generate and Log OTP
        otpService.generateOtp(phone);

        // Return success always for security/mocking
        return ResponseEntity.ok(Map.of("message", "OTP sent successfully. Check Console."));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> request) {
        String phone = request.get("phone");
        String otp = request.get("otp");

        if (phone == null || otp == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Phone and OTP are required"));
        }

        if (otpService.validateOtp(phone, otp)) {
            return ResponseEntity.ok(Map.of("success", true, "message", "OTP Verified Successfully"));
        } else {
            return ResponseEntity.status(400).body(Map.of("success", false, "message", "Invalid or Expired OTP"));
        }
    }
}
