package com.abhi.authProject.model;

import jakarta.persistence.Column; // Make sure this is imported
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table; // Add this import for @Table annotation
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "users") // Good practice to explicitly name your table
public class Users {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private int id;
    private String username;
    private String password;
    private String email;
    private String role;

    // --- START: CRITICAL CHANGES FOR isVerified FIELD ---
    @Column(name = "is_verified", nullable = false)
    private boolean isVerified = false; // Initialize to false directly. This is the default value for new users.

    // Also consider adding a constructor to ensure it's false when creating new
    // instances programmatically
    // If you're using Lombok's @Builder, ensure the builder also sets isVerified to
    // false by default.
    /*
     * public Users(String username, String password, String email, String role,
     * String verificationToken, LocalDateTime verificationTokenExpires) {
     * this.username = username;
     * this.password = password;
     * this.email = email;
     * this.role = role;
     * this.verificationToken = verificationToken;
     * this.verificationTokenExpires = verificationTokenExpires;
     * this.isVerified = false; // Explicitly set to false for new users
     * }
     */
    // --- END: CRITICAL CHANGES FOR isVerified FIELD ---

    private String verificationToken; // This will now store the OTP code
    private LocalDateTime verificationTokenExpires;

    @Column(name = "profile_picture_url", columnDefinition = "TEXT")
    private String profilePictureUrl;

    @jakarta.persistence.OneToMany(mappedBy = "student", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    @lombok.ToString.Exclude
    private java.util.List<Application> applications;

    @jakarta.persistence.OneToOne(mappedBy = "user", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    @lombok.ToString.Exclude
    private StudentProfile studentProfile;

    @jakarta.persistence.OneToOne(mappedBy = "user", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    @lombok.ToString.Exclude
    private ResumeFile resumeFile;

    @jakarta.persistence.OneToOne(mappedBy = "user", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    @lombok.ToString.Exclude
    private PasswordResetToken passwordResetToken;

    // Field to link Company Admin to a specific company
    private String companyName;

    // Field to enable/disable Company Admin (Super Admin control)
    @Column(name = "enabled", nullable = false)
    private boolean enabled = true; // Default: enabled

    // Field for DEPT_ADMIN to specify which branch/department they manage
    @Column(name = "admin_branch")
    private String adminBranch; // e.g., "MCA", "BCA", "IMCA", "B.Tech CSE"

    // Field for COMPANY_ADMIN to specify which departments they can post jobs for
    // Stored as comma-separated values: "MCA,BCA,IMCA"
    @Column(name = "allowed_departments", length = 500)
    private String allowedDepartments; // e.g., "MCA,BCA,IMCA"

    // Unique computer code for students (enrollment/roll number)
    @Column(name = "computer_code", unique = true)
    private String computerCode; // e.g., "MCA2023001", "BCA2024015"

    // Student profile fields
    @Column(name = "name")
    private String name; // Full name

    @Column(name = "phone")
    private String phone; // Phone number

    // NEW: Aadhar Number for unique identity tracking
    @Column(name = "aadhar_number", unique = true)
    private String aadharNumber;

    // NEW: Date of Birth (from Aadhar verification)
    @Column(name = "dob")
    private String dob; // Format: DD/MM/YYYY or YYYY-MM-DD

    // NEW: Gender (from Aadhar verification)
    @Column(name = "gender")
    private String gender; // Male/Female

    // Branch/Semester filtering fields
    @Column(name = "branch")
    private String branch; // IMCA, MCA, BCA

    @Column(name = "semester")
    private Integer semester; // IMCA: 1-10, MCA: 1-4, BCA: 2,4,6 (Year*2)

    @Column(name = "batch")
    private String batch; // e.g., "2022-2027"

    @Column(name = "last_profile_update")
    private java.time.LocalDate lastProfileUpdate; // Track when student last updated profile

    @Column(name = "last_login_date")
    private LocalDateTime lastLoginDate;

    // Verified Identity Data (from ID card scan)
    @Column(name = "full_name")
    private String fullName;

    @Column(name = "father_name")
    private String fatherName;

    @Column(name = "institution")
    private String institution;

    @Column(name = "session")
    private String session; // e.g., "2022-2027"

    // Mobile Numbers (from ID card or manual entry)
    @Column(name = "mobile_primary")
    private String mobilePrimary;

    @Column(name = "mobile_secondary")
    private String mobileSecondary;

    // Additional Academic Info
    @Column(name = "enrollment_number")
    private String enrollmentNumber;

    @Column(name = "start_year")
    private String startYear; // Admission year

    // Images (Base64 or URL)
    @Column(name = "id_card_image", columnDefinition = "TEXT")
    private String idCardImage;

    @Column(name = "aadhar_card_image", columnDefinition = "TEXT")
    private String aadharCardImage;

    // NEW: Address from Aadhar
    @Column(name = "address", columnDefinition = "TEXT")
    private String address;
}