package com.abhi.authProject.controller;

import com.abhi.authProject.model.UserDto; // Import the new DTO
import com.abhi.authProject.model.Users;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors; // Import collectors

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'COMPANY_ADMIN')") // Allow View for Company Admin
public class AdminUserController {

    @Autowired
    private com.abhi.authProject.repo.UserRepo userRepo;

    @Autowired
    private com.abhi.authProject.service.EmailService emailService;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @GetMapping("/users")
    public ResponseEntity<List<UserDto>> getAllUsers(java.security.Principal principal) {
        List<Users> users = userRepo.findAll();

        // Filter users based on role
        if (principal != null) {
            String username = principal.getName();
            Users currentUser = userRepo.findByUsername(username).orElse(null);

            if (currentUser != null && "DEPT_ADMIN".equals(currentUser.getRole())) {
                // DEPT_ADMIN: Only show students from their branch
                String adminBranch = currentUser.getAdminBranch();
                if (adminBranch != null && !adminBranch.isEmpty()) {
                    users = users.stream()
                            .filter(user -> {
                                // Show only students (USER role) from their branch
                                if ("USER".equals(user.getRole())) {
                                    return adminBranch.equals(user.getBranch());
                                }
                                return false;
                            })
                            .collect(Collectors.toList());
                }
            } else if (currentUser != null && "COMPANY_ADMIN".equals(currentUser.getRole())) {
                // COMPANY_ADMIN: Only show students from their allowed departments
                String allowedDepartments = currentUser.getAllowedDepartments();
                if (allowedDepartments != null && !allowedDepartments.isEmpty()) {
                    String[] allowedDepts = allowedDepartments.split(",");
                    users = users.stream()
                            .filter(user -> {
                                // Show only students (USER role) from allowed departments
                                if ("USER".equals(user.getRole()) && user.getBranch() != null) {
                                    for (String dept : allowedDepts) {
                                        if (user.getBranch().trim().equalsIgnoreCase(dept.trim())) {
                                            return true;
                                        }
                                    }
                                }
                                return false;
                            })
                            .collect(Collectors.toList());
                }
            }
        }

        List<UserDto> userDtos = users.stream()
                .map(user -> new UserDto(
                        user.getId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getRole(),
                        user.isVerified(),
                        user.getCompanyName(),
                        user.isEnabled(),
                        user.getBranch(),
                        user.getSemester(),
                        user.getAdminBranch(),
                        user.getAllowedDepartments(),
                        user.getComputerCode(),
                        user.getBatch()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(userDtos);
    }

    @PostMapping("/users")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'DEPT_ADMIN')")
    public ResponseEntity<?> createUser(@RequestBody Users user, java.security.Principal principal) {
        try {
            // Role-based creation limits
            if (principal != null) {
                String username = principal.getName();
                Users currentUser = userRepo.findByUsername(username).orElse(null);

                if (currentUser != null && "DEPT_ADMIN".equals(currentUser.getRole())) {
                    // DEPT_ADMIN can only create students (USER role) for their branch
                    if (!"USER".equals(user.getRole())) {
                        return ResponseEntity.status(403).body("DEPT_ADMIN can only create student (USER) accounts");
                    }
                    user.setBranch(currentUser.getAdminBranch());
                    // Force the branch to match the admin's branch
                }
            }

            // Basic validation
            if (userRepo.findByUsername(user.getUsername()).isPresent()) {
                return ResponseEntity.badRequest().body("Username already exists");
            }
            if (userRepo.findByEmail(user.getEmail()).isPresent()) {
                return ResponseEntity.badRequest().body("Email already exists");
            }

            // DEPT_ADMIN Creation Validation: Only ONE DEPT_ADMIN per branch
            if ("DEPT_ADMIN".equals(user.getRole())) {
                if (user.getAdminBranch() == null || user.getAdminBranch().isEmpty()) {
                    return ResponseEntity.badRequest().body("Admin branch is required for DEPT_ADMIN role");
                }

                System.out.println("[ADMIN API] Attempting to create user: " + user.getUsername() +
                        " | Role: " + user.getRole() +
                        " | Branch: " + user.getAdminBranch());

                // Check if a DEPT_ADMIN already exists for this branch
                List<Users> existingDeptAdmins = userRepo.findByRoleAndAdminBranch("DEPT_ADMIN", user.getAdminBranch());
                if (!existingDeptAdmins.isEmpty()) {
                    String existingAdmin = existingDeptAdmins.get(0).getUsername();
                    System.out.println(
                            "[ADMIN API] Creation rejected: DEPT_ADMIN already exists for " + user.getAdminBranch());
                    return ResponseEntity.badRequest().body(
                            "A Department Admin already exists for " + user.getAdminBranch() +
                                    " (" + existingAdmin + "). Only one DEPT_ADMIN is allowed per department.");
                }
            }

            // COMPANY_ADMIN Validation: Require allowedDepartments
            if ("COMPANY_ADMIN".equals(user.getRole())) {
                if (user.getAllowedDepartments() == null || user.getAllowedDepartments().isEmpty()) {
                    return ResponseEntity.badRequest().body("Allowed departments are required for COMPANY_ADMIN role");
                }
            }

            String rawPassword = user.getPassword(); // Capture raw password for email

            // Encrypt password if provided (force it if new user)
            if (user.getPassword() != null && !user.getPassword().isEmpty()) {
                user.setPassword(passwordEncoder.encode(user.getPassword()));
            } else if (!"DEPT_ADMIN".equals(user.getRole())) { // Allow blank password ONLY if placeholder or special
                                                               // case
                return ResponseEntity.badRequest().body("Password is required for new accounts");
            }

            // AUTO-VERIFY: Any user created by an Admin should be verified by default
            user.setVerified(true);

            Users savedUser = userRepo.save(user);

            // Send Welcome Email
            try {
                if (rawPassword != null && !rawPassword.isEmpty()) {
                    emailService.sendAccountCreatedEmail(savedUser.getEmail(), savedUser.getUsername(),
                            savedUser.getRole(),
                            rawPassword);
                }
            } catch (Exception e) {
                // Log but don't fail the request
                System.err.println("Failed to send welcome email: " + e.getMessage());
            }

            return ResponseEntity.ok(new UserDto(
                    savedUser.getId(),
                    savedUser.getUsername(),
                    savedUser.getEmail(),
                    savedUser.getRole(),
                    savedUser.isVerified(),
                    savedUser.getCompanyName(),
                    savedUser.isEnabled(),
                    savedUser.getBranch(),
                    savedUser.getSemester(),
                    savedUser.getAdminBranch(),
                    savedUser.getAllowedDepartments(),
                    savedUser.getComputerCode(),
                    savedUser.getBatch()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to create user: " + e.getMessage());
        }
    }

    @PutMapping("/users/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'DEPT_ADMIN')")
    public ResponseEntity<?> updateUser(@PathVariable Integer id, @RequestBody Users updatedUser,
            java.security.Principal principal) {
        return userRepo.findById(id)
                .map(user -> {
                    // DEPT_ADMIN validation: Can only update students from their branch
                    if (principal != null) {
                        String username = principal.getName();
                        Users currentUser = userRepo.findByUsername(username).orElse(null);

                        if (currentUser != null && "DEPT_ADMIN".equals(currentUser.getRole())) {
                            String adminBranch = currentUser.getAdminBranch();

                            // Check if user being updated is a student from their branch
                            if (!"USER".equals(user.getRole()) || !adminBranch.equals(user.getBranch())) {
                                return ResponseEntity.status(403)
                                        .body("Access Denied: You can only update students from your department ("
                                                + adminBranch + ")");
                            }

                            // DEPT_ADMIN cannot change student's branch or role
                            if (!user.getBranch().equals(updatedUser.getBranch()) ||
                                    !user.getRole().equals(updatedUser.getRole())) {
                                return ResponseEntity.badRequest()
                                        .body("DEPT_ADMIN cannot change student's branch or role");
                            }
                        }
                    }

                    user.setUsername(updatedUser.getUsername());
                    user.setEmail(updatedUser.getEmail());
                    user.setRole(updatedUser.getRole());

                    // Fix for accidental lockout: If becoming Admin/SuperAdmin, ensure verified is
                    // true
                    if ("ADMIN".equals(updatedUser.getRole()) || "SUPER_ADMIN".equals(updatedUser.getRole())
                            || "COMPANY_ADMIN".equals(updatedUser.getRole())) {
                        user.setVerified(true);
                    } else {
                        user.setVerified(updatedUser.isVerified());
                    }

                    user.setCompanyName(updatedUser.getCompanyName()); // Update company
                    user.setBranch(updatedUser.getBranch());
                    user.setSemester(updatedUser.getSemester());
                    user.setAdminBranch(updatedUser.getAdminBranch()); // Update admin branch
                    user.setAllowedDepartments(updatedUser.getAllowedDepartments()); // Update allowed departments

                    // Only update password if new one is provided
                    if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
                        user.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
                    }

                    Users saved = userRepo.save(user);
                    return ResponseEntity.ok(new UserDto(
                            saved.getId(),
                            saved.getUsername(),
                            saved.getEmail(),
                            saved.getRole(),
                            saved.isVerified(),
                            saved.getCompanyName(),
                            saved.isEnabled(),
                            saved.getBranch(),
                            saved.getSemester(),
                            saved.getAdminBranch(),
                            saved.getAllowedDepartments(),
                            saved.getComputerCode(),
                            saved.getBatch()));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'DEPT_ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Integer id, java.security.Principal principal) {
        // DEPT_ADMIN validation: Can only delete students from their branch
        if (principal != null) {
            String username = principal.getName();
            Users currentUser = userRepo.findByUsername(username).orElse(null);

            if (currentUser != null && "DEPT_ADMIN".equals(currentUser.getRole())) {
                Users userToDelete = userRepo.findById(id).orElse(null);

                if (userToDelete != null) {
                    String adminBranch = currentUser.getAdminBranch();

                    // Check if user being deleted is a student from their branch
                    if (!"USER".equals(userToDelete.getRole()) || !adminBranch.equals(userToDelete.getBranch())) {
                        return ResponseEntity.status(403)
                                .body("Access Denied: You can only delete students from your department (" + adminBranch
                                        + ")");
                    }
                }
            }
        }

        if (userRepo.existsById(id)) {
            userRepo.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    // Toggle company enabled/disabled status (Super Admin only)
    @PutMapping("/users/{id}/toggle-status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> toggleCompanyStatus(@PathVariable Integer id) {
        return userRepo.findById(id)
                .map(user -> {
                    // Toggle the enabled status
                    user.setEnabled(!user.isEnabled());
                    Users saved = userRepo.save(user);

                    return ResponseEntity.ok(new java.util.HashMap<String, Object>() {
                        {
                            put("id", saved.getId());
                            put("username", saved.getUsername());
                            put("companyName", saved.getCompanyName());
                            put("enabled", saved.isEnabled());
                            put("message", saved.isEnabled() ? "Company enabled successfully"
                                    : "Company disabled successfully");
                        }
                    });
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Get students grouped by branch, semester, or batch
    @GetMapping("/students/grouped")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'DEPT_ADMIN', 'COMPANY_ADMIN')")
    public ResponseEntity<?> getStudentsGrouped(
            @RequestParam(defaultValue = "branch") String groupBy,
            java.security.Principal principal) {

        // Get all students (USER role only)
        List<Users> students = userRepo.findAll().stream()
                .filter(user -> "USER".equals(user.getRole()))
                .collect(Collectors.toList());

        // Filter based on role
        if (principal != null) {
            String username = principal.getName();
            Users currentUser = userRepo.findByUsername(username).orElse(null);

            if (currentUser != null && "DEPT_ADMIN".equals(currentUser.getRole())) {
                // DEPT_ADMIN: Only their branch students
                String adminBranch = currentUser.getAdminBranch();
                students = students.stream()
                        .filter(s -> adminBranch.equals(s.getBranch()))
                        .collect(Collectors.toList());
            } else if (currentUser != null && "COMPANY_ADMIN".equals(currentUser.getRole())) {
                // COMPANY_ADMIN: Only allowed departments
                String allowedDepartments = currentUser.getAllowedDepartments();
                if (allowedDepartments != null && !allowedDepartments.isEmpty()) {
                    String[] allowedDepts = allowedDepartments.split(",");
                    students = students.stream()
                            .filter(s -> {
                                if (s.getBranch() != null) {
                                    for (String dept : allowedDepts) {
                                        if (s.getBranch().trim().equalsIgnoreCase(dept.trim())) {
                                            return true;
                                        }
                                    }
                                }
                                return false;
                            })
                            .collect(Collectors.toList());
                }
            }
        }

        // Group students
        java.util.Map<String, List<UserDto>> groups = new java.util.LinkedHashMap<>();

        for (Users student : students) {
            String key = "";

            switch (groupBy.toLowerCase()) {
                case "branch":
                    key = student.getBranch() != null ? student.getBranch() : "Unknown";
                    break;
                case "semester":
                    key = student.getSemester() != null ? "Semester " + student.getSemester() : "Unknown";
                    break;
                case "batch":
                    key = student.getBatch() != null ? student.getBatch() : "Unknown";
                    break;
                default:
                    key = "All Students";
            }

            groups.putIfAbsent(key, new java.util.ArrayList<>());
            groups.get(key).add(new UserDto(
                    student.getId(),
                    student.getUsername(),
                    student.getEmail(),
                    student.getRole(),
                    student.isVerified(),
                    student.getCompanyName(),
                    student.isEnabled(),
                    student.getBranch(),
                    student.getSemester(),
                    student.getAdminBranch(),
                    student.getAllowedDepartments(),
                    student.getComputerCode(),
                    student.getBatch()));
        }

        // Create response
        com.abhi.authProject.model.StudentGroupedDto response = new com.abhi.authProject.model.StudentGroupedDto();
        response.setGroupBy(groupBy);
        response.setGroups(groups);
        response.setTotalStudents(students.size());

        return ResponseEntity.ok(response);
    }
}