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
import org.springframework.transaction.annotation.Transactional; // Import for transactional methods

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;
import java.util.UUID; // For generating a more secure password reset token

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

    // --- Helper Methods ---

    /**
     * Generates a random 6-digit numeric OTP for email verification.
     * @return The generated OTP as a String.
     */
    private String generateOTP() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000); // 6-digit OTP
        return String.valueOf(otp);
    }

    /**
     * Generates a unique, secure token for password reset.
     * Using UUID for higher security than a simple numeric OTP for password resets.
     * @return A unique token string.
     */
    private String generatePasswordResetToken() {
        return UUID.randomUUID().toString();
    }

    // --- User Management Methods ---

    /**
     * Registers a new user, hashes password, generates OTP,
     * and sends a verification email with the OTP.
     * @param user The Users object containing registration details.
     * @return The saved Users object.
     * @throws IllegalArgumentException if username or email already exists.
     */
    @Transactional // Ensure atomicity for save operations
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

        // Generate and set OTP and expiry for EMAIL VERIFICATION
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
        // First, check if the user exists and is verified
        Optional<Users> userOptional = repo.findByUsername(username);

        if (userOptional.isEmpty()) {
            // Throw BadCredentialsException for consistency with Spring Security's behavior for non-existent users
            throw new BadCredentialsException("Invalid username or password");
        }

        Users user = userOptional.get();

        // Check if the user's email is verified
        if (!user.isVerified()) {
            throw new IllegalStateException("Please verify your email address with the code to log in.");
        }

        // Now, attempt to authenticate with Spring Security
        Authentication authentication = authManager.authenticate(
            new UsernamePasswordAuthenticationToken(username, password)
        );

        // If authentication is successful (no exception thrown), generate token
        if (authentication.isAuthenticated()) {
            return jwtservice.generateToken(username);
        } else {
            // This line should ideally not be reached if authenticate() throws exceptions on failure,
            // but kept for defensive programming.
            throw new BadCredentialsException("Authentication failed");
        }
    }

    /**
     * Verifies a user's account using the provided OTP code.
     * @param identifier The username or email of the user trying to verify.
     * @param otpCode The OTP code entered by the user.
     * @return true if verification is successful, false otherwise.
     */
    @Transactional
    public boolean verifyAccountWithCode(String identifier, String otpCode) {
        Optional<Users> userOptional = repo.findByUsername(identifier);

        if (userOptional.isEmpty()) {
            userOptional = repo.findByEmail(identifier);
        }

        if (userOptional.isEmpty()) {
            return false; // User not found with given identifier
        }

        Users user = userOptional.get();

        if (user.isVerified()) {
            return true; // Already verified, consider it a success
        }

        // Check if OTP matches and is not expired
        if (user.getVerificationToken() != null && user.getVerificationToken().equals(otpCode) &&
            user.getVerificationTokenExpires() != null && user.getVerificationTokenExpires().isAfter(LocalDateTime.now())) {

            user.setVerified(true);
            user.setVerificationToken(null);
            user.setVerificationTokenExpires(null);
            repo.save(user);
            return true;
        }

        return false; // OTP mismatch or expired
    }


    /**
     * Initiates the password reset process for a user.
     * Generates a unique token, saves it, and sends a password reset email.
     * @param emailOrUsername The email or username of the user requesting a password reset.
     * @throws IllegalArgumentException if the user is not found or email sending fails.
     */
   // Inside UserService.java

@Transactional
public void initiatePasswordReset(String emailOrUsername) {
    // --- ADD THIS LINE AT THE VERY TOP OF THE METHOD ---
    System.out.println("UserService.initiatePasswordReset received identifier: '" + emailOrUsername + "'");
    // --- END ADDITION ---

    // Try to find the user by username or email
    Optional<Users> userOptional = repo.findByUsername(emailOrUsername);
    if (userOptional.isEmpty()) {
        userOptional = repo.findByEmail(emailOrUsername);
    }

    if (userOptional.isEmpty()) {
        System.out.println("Password reset request for non-existent user: " + emailOrUsername + ". Returning generic success.");
        return;
    }
    // ... rest of the method
}

        Users user = userOptional.get();

        // Generate a new, secure password reset token
        String resetToken = generatePasswordResetToken();
        // Set expiry for the reset token (e.g., 30 minutes)
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(30);

        user.setPasswordResetToken(resetToken);
        user.setPasswordResetTokenExpires(expiryTime);
        repo.save(user); // Save the user with the new token and expiry

        // Construct the password reset link
        // This assumes your frontend's reset password page is at /reset-password.html?token=...
        // Make sure to replace `https://hack-2-hired.onrender.com` with your actual frontend domain
        String resetLink = "https://hack-2-hired.onrender.com/reset-password.html?token=" + resetToken;

        String emailSubject = "Placement Portal - Password Reset Request";
        String emailBody = "Dear " + user.getUsername() + ",\n\n"
                         + "You have requested to reset your password for your Placement Portal account.\n\n"
                         + "Please click on the following link to reset your password:\n"
                         + resetLink + "\n\n"
                         + "This link will expire in 30 minutes.\n\n"
                         + "If you did not request a password reset, please ignore this email.\n\n"
                         + "Best regards,\n"
                         + "Placement Portal Team";

        try {
            emailService.sendEmail(user.getEmail(), emailSubject, emailBody);
        } catch (Exception e) {
            // Log the error but don't re-throw if you want to keep the generic success message
            System.err.println("Failed to send password reset email to " + user.getEmail() + ": " + e.getMessage());
            // Optionally, you might want to remove the token from the user if email sending truly failed and it's critical
            // user.setPasswordResetToken(null);
            // user.setPasswordResetTokenExpires(null);
            // repo.save(user);
            throw new RuntimeException("Could not send password reset email. Please try again later.", e);
        }
    }

    /**
     * Resets a user's password using a valid reset token.
     * @param token The password reset token received from the email link.
     * @param newPassword The new password provided by the user.
     * @throws IllegalArgumentException if the token is invalid, expired, or the new password is empty.
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("New password cannot be empty.");
        }
        // Add more robust password policy checks here (e.g., min length, complexity)
        // Example: if (newPassword.length() < 8) { throw new IllegalArgumentException("Password must be at least 8 characters long."); }

        Optional<Users> userOptional = repo.findByPasswordResetToken(token);

        if (userOptional.isEmpty()) {
            throw new IllegalArgumentException("Invalid or unrecognized password reset token.");
        }

        Users user = userOptional.get();

        // Check if the token has expired
        if (user.getPasswordResetTokenExpires() == null || user.getPasswordResetTokenExpires().isBefore(LocalDateTime.now())) {
            // Invalidate the expired token to prevent reuse attempts
            user.setPasswordResetToken(null);
            user.setPasswordResetTokenExpires(null);
            repo.save(user);
            throw new IllegalArgumentException("Password reset token has expired. Please request a new one.");
        }

        // Hash the new password and update the user's password
        user.setPassword(encoder.encode(newPassword));

        // Clear the used token and its expiry
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpires(null);

        repo.save(user); // Save the updated user
    }
}