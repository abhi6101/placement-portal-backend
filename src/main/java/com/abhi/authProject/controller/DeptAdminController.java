package com.abhi.authProject.controller;

import com.abhi.authProject.model.Users;
import com.abhi.authProject.repo.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dept-admin")
@CrossOrigin
@PreAuthorize("hasRole('DEPT_ADMIN') or hasRole('SUPER_ADMIN')")
public class DeptAdminController {

    @Autowired
    private UserRepo userRepo;

    // Get My Department's Students
    @GetMapping("/students")
    public ResponseEntity<?> getMyDepartmentStudents(Authentication auth) {
        String username = auth.getName();
        Users admin = userRepo.findByUsername(username).orElseThrow(() -> new RuntimeException("Admin not found"));

        // If Super Admin, show all
        if ("SUPER_ADMIN".equals(admin.getRole())) {
            List<Users> allStudents = userRepo.findAll().stream()
                    .filter(u -> "USER".equals(u.getRole()))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(allStudents);
        }

        // If Dept Admin, show only matching branch
        String myBranch = admin.getBranch();
        if (myBranch == null || myBranch.isEmpty()) {
            return ResponseEntity.badRequest().body("You are a Dept Admin but have no assigned Branch!");
        }

        List<Users> myStudents = userRepo.findAll().stream()
                .filter(u -> "USER".equals(u.getRole()) && myBranch.equalsIgnoreCase(u.getBranch()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(myStudents);
    }

    // Assign a new Department Admin (Super Admin Only)
    @PostMapping("/create-admin")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> createDeptAdmin(@RequestBody Users user) {
        // Logic to create a user with role DEPT_ADMIN would go here
        // Reusing existing AuthController register logic is better, but this endpoint
        // could be used to specifically promote an existing user or specific simplified
        // flow.
        // For now, we assume Super Admin uses standard register API with
        // role='DEPT_ADMIN'
        return ResponseEntity.status(501).body("Use /api/auth/register with role DEPT_ADMIN");
    }
}
