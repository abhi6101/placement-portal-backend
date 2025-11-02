package com.abhi.authProject.service;

import com.abhi.authProject.Jwt.JWTService;
import com.abhi.authProject.model.Users;
import com.abhi.authProject.repo.UserRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepo repo;

    @Autowired
    private JWTService jwtservice;

    @Autowired
    private AuthenticationManager authManager;

    @Autowired
    private BCryptPasswordEncoder encoder;

    // REMOVED: The old EmailService is gone.
    // @Autowired
    // private EmailService emailService;

    // ADDED: The new SendGridEmailService.
    @Autowired
    private SendGridEmailService emailService;

    private String generateOTP() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    public Users registerUser(Users user) {
        if (repo.findByUsername(user.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (repo.findByEmail(user.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already registered");
        }

        user.setPassword(encoder.encode(user.getPassword()));
        user.setVerified(false);
        String otp = generateOTP();
        user.setVerificationToken(otp);
        user.setVerificationTokenExpires(LocalDateTime.now().plusMinutes(15));

        Users savedUser = repo.save(user);

        String emailSubject = "Placement Portal Account Verification Code (OTP)";
        // Using HTML for better formatting
        String emailBody = "<h3>Welcome to the Placement Portal!</h3>"
                         + "<p>Dear " + user.getUsername() + ",</p>"
                         + "<p>Thank you for registering. Your verification code is:</p>"
                         + "<h2 style='color: #333;'>" + otp + "</h2>"
                         + "<p>This code will expire in 15 minutes. Please enter it on the verification page to activate your account.</p>"
                         + "<p>If you did not register for this account, please ignore this email.</p>"
                         + "<br/><p>Best regards,<br/>The Placement Portal Team</p>";

        try {
            // Use the new service. The 'null' means there is no attachment.
            emailService.sendEmailWithAttachment(user.getEmail(), emailSubject, emailBody, null);
            logger.info("Verification OTP sent to {}", user.getEmail());
        } catch (IOException e) {
            logger.error("User {} was registered, but the verification email failed to send.", savedUser.getId(), e);
            // In a real app, you might want to handle this more gracefully,
            // e.g., by allowing the user to request a new OTP later.
        }

        return savedUser;
    }

    public String verifyAndLogin(String username, String password) {
        Authentication authentication = authManager.authenticate(
            new UsernamePasswordAuthenticationToken(username, password)
        );

        Optional<Users> userOptional = repo.findByUsername(username);

        if (userOptional.isEmpty()) {
            throw new BadCredentialsException("User not found after authentication.");
        }

        Users user = userOptional.get();

        if (!user.isVerified()) {
            throw new IllegalStateException("Please verify your email address with the code to log in.");
        }

        if (authentication.isAuthenticated()) {
            return jwtservice.generateToken(username);
        } else {
            throw new BadCredentialsException("Authentication failed");
        }
    }

    public boolean verifyAccountWithCode(String identifier, String otpCode) {
        Optional<Users> userOptional = repo.findByUsername(identifier);

        if (userOptional.isEmpty()) {
            userOptional = repo.findByEmail(identifier);
        }

        if (userOptional.isEmpty()) {
            return false;
        }

        Users user = userOptional.get();

        if (user.isVerified()) {
            return true;
        }

        if (user.getVerificationToken() != null && user.getVerificationToken().equals(otpCode) &&
            user.getVerificationTokenExpires() != null && user.getVerificationTokenExpires().isAfter(LocalDateTime.now())) {

            user.setVerified(true);
            user.setVerificationToken(null);
            user.setVerificationTokenExpires(null);
            repo.save(user);
            return true;
        }

        return false;
    }
}