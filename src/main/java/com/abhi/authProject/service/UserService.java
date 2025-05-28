package com.abhi.authProject.service;

import com.abhi.authProject.Jwt.JWTService;
import com.abhi.authProject.model.Users;
import com.abhi.authProject.repo.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random; // For generating OTP

@Service
public class UserService {

    @Autowired
    private UserRepo repo;

    @Autowired
    private JWTService jwtservice;

    @Autowired
    private AuthenticationManager authManager;

    @Autowired
    private BCryptPasswordEncoder encoder;

    @Autowired
    private EmailService emailService;

    /**
     * Generates a random 6-digit numeric OTP.
     * @return The generated OTP as a String.
     */
    private String generateOTP() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000); // 6-digit OTP
        return String.valueOf(otp);
    }

    /**
     * Registers a new user, hashes password, generates OTP,
     * and sends a verification email with the OTP.
     * @param user The Users object containing registration details.
     * @return The saved Users object.
     * @throws IllegalArgumentException if username or email already exists.
     */
    public Users registerUser(Users user) {
        // Check if username already exists
        if (repo.findByUsername(user.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }

        // Check if email already exists
        if (repo.findByEmail(user.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already registered");
        }

        // Encode password
        user.setPassword(encoder.encode(user.getPassword()));

        // Set initial verification status to false
        user.setVerified(false);

        // Generate and set OTP and expiry
        String otp = generateOTP();
        user.setVerificationToken(otp); // Store OTP in verificationToken field
        user.setVerificationTokenExpires(LocalDateTime.now().plusMinutes(15)); // OTP valid for 15 minutes

        // Save the user
        Users savedUser = repo.save(user);

        // Send OTP email
        String emailSubject = "Placement Portal Account Verification Code (OTP)";
        String emailBody = "Dear " + user.getUsername() + ",\n\n"
                         + "Thank you for registering with Placement Portal. Your verification code is:\n\n"
                         + "OTP: " + otp + "\n\n"
                         + "This code will expire in 15 minutes. Please enter it on the verification page to activate your account.\n\n"
                         + "If you did not register for this account, please ignore this email.\n\n"
                         + "Best regards,\n"
                         + "Placement Portal Team";

        emailService.sendEmail(user.getEmail(), emailSubject, emailBody);

        return savedUser;
    }

    /**
     * Authenticates a user and generates a JWT token.
     * Checks if the user's email is verified before allowing login.
     * @param username The username.
     * @param password The password.
     * @return JWT token if authentication successful and email verified.
     * @throws BadCredentialsException if authentication fails.
     * @throws IllegalStateException if email is not verified.
     */
    public String verifyAndLogin(String username, String password) {
        Authentication authentication = authManager.authenticate(
            new UsernamePasswordAuthenticationToken(username, password)
        );

        Optional<Users> userOptional = repo.findByUsername(username);

        if (userOptional.isEmpty()) {
            throw new BadCredentialsException("User not found after authentication.");
        }

        Users user = userOptional.get();

        // Check if the user's email is verified
        if (!user.isVerified()) {
            throw new IllegalStateException("Please verify your email address with the code to log in.");
        }

        if (authentication.isAuthenticated()) {
            return jwtservice.generateToken(username);
        } else {
            throw new BadCredentialsException("Authentication failed");
        }
    }

    /**
     * Verifies a user's account using the provided OTP code.
     * @param identifier The username or email of the user trying to verify.
     * @param otpCode The OTP code entered by the user.
     * @return true if verification is successful, false otherwise.
     */
    public boolean verifyAccountWithCode(String identifier, String otpCode) {
        Optional<Users> userOptional = repo.findByUsername(identifier); // Try finding by username first

        if (userOptional.isEmpty()) {
            userOptional = repo.findByEmail(identifier); // If not found by username, try by email
        }

        if (userOptional.isEmpty()) {
            return false; // User not found with given identifier
        }

        Users user = userOptional.get();

        // Check if already verified
        if (user.isVerified()) {
            return true; // Already verified, consider it a success
        }

        // Check if OTP matches and is not expired
        if (user.getVerificationToken() != null && user.getVerificationToken().equals(otpCode) &&
            user.getVerificationTokenExpires() != null && user.getVerificationTokenExpires().isAfter(LocalDateTime.now())) {

            // Mark user as verified
            user.setVerified(true);
            user.setVerificationToken(null); // Clear the OTP after use
            user.setVerificationTokenExpires(null); // Clear expiry
            repo.save(user);
            return true;
        }

        return false; // OTP mismatch or expired
    }
}