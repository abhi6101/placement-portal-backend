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
import java.util.UUID; // For generating unique tokens

@Service
public class UserService { // Renamed from AuthService for consistency with your provided code

    @Autowired
    private UserRepo repo;

    @Autowired
    private JWTService jwtservice;

    @Autowired
    private AuthenticationManager authManager;

    @Autowired
    private BCryptPasswordEncoder encoder;

    @Autowired
    private EmailService emailService; // NEW: Inject EmailService

    /**
     * Registers a new user, hashes password, generates verification token,
     * and sends a verification email.
     * @param user The Users object containing registration details.
     * @return The saved Users object.
     * @throws IllegalArgumentException if username or email already exists.
     */
    public Users registerUser(Users user) { // Renamed from 'register' for clarity
        // Check if username already exists
        if (repo.findByUsername(user.getUsername()).isPresent()) { // Use Optional.isPresent()
            throw new IllegalArgumentException("Username already exists");
        }

        // Check if email already exists
        if (repo.findByEmail(user.getEmail()).isPresent()) { // Use Optional.isPresent()
            throw new IllegalArgumentException("Email already registered");
        }

        // Encode password
        user.setPassword(encoder.encode(user.getPassword()));

        // NEW: Set initial verification status to false
        user.setVerified(false);

        // NEW: Generate and set verification token and expiry
        String verificationToken = UUID.randomUUID().toString();
        user.setVerificationToken(verificationToken);
        user.setVerificationTokenExpires(LocalDateTime.now().plusHours(24)); // Token valid for 24 hours

        // Save the user
        Users savedUser = repo.save(user);

        // NEW: Send verification email
        // IMPORTANT: Replace the base URL with your actual backend's public URL (e.g., your Render URL)
        String verificationLink = "https://placement-portal-backend-nwaj.onrender.com/api/auth/verify-email?token=" + verificationToken;
        String emailSubject = "Verify your Placement Portal Account";
        String emailBody = "Dear " + user.getUsername() + ",\n\n"
                         + "Thank you for registering with Placement Portal. Please click the link below to verify your email address:\n\n"
                         + verificationLink + "\n\n"
                         + "This link will expire in 24 hours.\n\n"
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
    public String verifyAndLogin(String username, String password) { // Renamed from 'verify' for clarity in login context
        Authentication authentication = authManager.authenticate(
            new UsernamePasswordAuthenticationToken(username, password)
        );

        // Get the user from the repository after authentication
        Optional<Users> userOptional = repo.findByUsername(username);

        if (userOptional.isEmpty()) {
            // This case should ideally not happen if authentication was successful
            // but is a good safeguard.
            throw new BadCredentialsException("User not found after authentication.");
        }

        Users user = userOptional.get();

        // NEW: Check if the user's email is verified
        if (!user.isVerified()) {
            throw new IllegalStateException("Please verify your email address to log in.");
        }

        if (authentication.isAuthenticated()) {
            return jwtservice.generateToken(username);
        } else {
            // This path might not be reached if authenticate() throws BadCredentialsException
            throw new BadCredentialsException("Authentication failed");
        }
    }

    /**
     * Verifies a user's email using the provided token.
     * @param token The verification token from the email link.
     * @return true if verification is successful, false otherwise.
     */
    public boolean verifyEmail(String token) {
        Optional<Users> userOptional = repo.findByVerificationToken(token);

        if (userOptional.isEmpty()) {
            return false; // Token not found
        }

        Users user = userOptional.get();

        // Check if token has expired
        if (user.getVerificationTokenExpires() != null && user.getVerificationTokenExpires().isBefore(LocalDateTime.now())) {
            // Optionally, you might want to remove the expired token here
            // user.setVerificationToken(null);
            // user.setVerificationTokenExpires(null);
            // repo.save(user); // If you uncomment the above, uncomment this
            return false; // Token expired
        }

        // Check if already verified (optional, but good for idempotency)
        if (user.isVerified()) {
            return true; // Already verified, consider it a success
        }

        // Mark user as verified
        user.setVerified(true);
        user.setVerificationToken(null); // Clear the token after use
        user.setVerificationTokenExpires(null); // Clear expiry
        repo.save(user);
        return true;
    }
}