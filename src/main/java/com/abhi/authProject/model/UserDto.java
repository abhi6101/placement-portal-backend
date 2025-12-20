package com.abhi.authProject.model;

// Using Lombok for simplicity, or generate getters/setters manually
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {
    private int id;
    private String username;
    private String email;
    private String role;
    private boolean isVerified;
    private String companyName;
    private boolean enabled; // For company enable/disable feature
    private String branch; // IMCA, MCA, BCA (for students)
    private Integer semester; // Student's semester/year
    private String adminBranch; // For DEPT_ADMIN - which branch they manage
    private String allowedDepartments; // For COMPANY_ADMIN - which departments they can post jobs for
    private String computerCode; // Unique student identifier (e.g., 59500)
    private String batch; // Passout year (e.g., 2027, 2026)
}