package com.abhi.authProject.service;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Random;

@Service
public class OtpService {

    // Simulating a database/cache for OTPs (Phone -> OTP)
    // Using ConcurrentHashMap for thread safety in simple in-memory storage
    private final Map<String, String> otpStorage = new ConcurrentHashMap<>();

    public String generateOtp(String phoneNumber) {
        // Generate 4-digit OTP
        String otp = String.format("%04d", new Random().nextInt(10000));

        // Store it
        otpStorage.put(phoneNumber, otp);

        // LOG IT TO CONSOLE (The "Free SMS" Bridge)
        System.out.println("\n\n");
        System.out.println("=========================================");
        System.out.println("📱 [SMS MOCK] OTP for " + phoneNumber + " is: " + otp);
        System.out.println("=========================================");
        System.out.println("\n\n");

        return otp;
    }

    public boolean validateOtp(String phoneNumber, String otp) {
        if (!otpStorage.containsKey(phoneNumber)) {
            return false;
        }
        String storedOtp = otpStorage.get(phoneNumber);
        if (storedOtp.equals(otp)) {
            otpStorage.remove(phoneNumber); // OTP is one-time use
            return true;
        }
        return false;
    }
}
