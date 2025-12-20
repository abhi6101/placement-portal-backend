package com.abhi.authProject.controller;

import com.abhi.authProject.model.InterviewDrive;
import com.abhi.authProject.repo.InterviewDriveRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("/api/interview-drives")
public class InterviewDriveController {

    @Autowired
    private com.abhi.authProject.repo.UserRepo userRepo;

    @Autowired
    private InterviewDriveRepo interviewDriveRepo;

    @GetMapping
    public List<InterviewDrive> getAllDrives() {
        // Return only future/today drives for cleaner list, or all?
        // Let's return all upcoming for now
        return interviewDriveRepo.findByDateAfterOrderByDateAsc(LocalDate.now().minusDays(1));
    }

    @PostMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'COMPANY_ADMIN', 'DEPT_ADMIN')")
    public InterviewDrive createDrive(@RequestBody InterviewDrive drive, java.security.Principal principal) {
        String username = principal.getName();
        com.abhi.authProject.model.Users user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if ("COMPANY_ADMIN".equals(user.getRole())) {
            // Check if company is enabled
            if (!user.isEnabled()) {
                throw new RuntimeException("Your company account has been disabled. Please contact the administrator.");
            }
            drive.setCompany(user.getCompanyName());

            // Restrict to allowed departments
            if (user.getAllowedDepartments() != null && !user.getAllowedDepartments().isEmpty()) {
                String[] allowedDepts = user.getAllowedDepartments().split(",");
                // Filter eligible branches to only include allowed departments
                if (drive.getEligibleBranches() != null) {
                    drive.getEligibleBranches().removeIf(branch -> {
                        for (String dept : allowedDepts) {
                            if (branch.trim().equalsIgnoreCase(dept.trim())) {
                                return false; // Keep this branch
                            }
                        }
                        return true; // Remove this branch
                    });
                }
            }
        } else if ("DEPT_ADMIN".equals(user.getRole())) {
            // DEPT_ADMIN can only post for their assigned branch
            if (user.getAdminBranch() != null && !user.getAdminBranch().isEmpty()) {
                // Override eligible branches to only their branch
                drive.setEligibleBranches(java.util.Arrays.asList(user.getAdminBranch()));
            } else {
                throw new RuntimeException("Department admin must have an assigned branch.");
            }
        }

        return interviewDriveRepo.save(drive);
    }

    @PutMapping("/admin/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'COMPANY_ADMIN', 'DEPT_ADMIN')")
    public ResponseEntity<?> updateDrive(@PathVariable Long id, @RequestBody InterviewDrive updatedDrive,
            java.security.Principal principal) {
        String username = principal.getName();
        com.abhi.authProject.model.Users user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return interviewDriveRepo.findById(id).map(drive -> {
            // Security Check
            if ("COMPANY_ADMIN".equals(user.getRole())) {
                if (!drive.getCompany().equals(user.getCompanyName())) {
                    return ResponseEntity.status(403).body("You are not authorized to update this interview drive.");
                }
                // Ensure they can't change the company name
                updatedDrive.setCompany(user.getCompanyName());
            } else if ("DEPT_ADMIN".equals(user.getRole())) {
                // DEPT_ADMIN can only update drives for their branch
                if (drive.getEligibleBranches() == null ||
                        !drive.getEligibleBranches().contains(user.getAdminBranch())) {
                    return ResponseEntity.status(403).body("You are not authorized to update this interview drive.");
                }
                // Ensure they can't change branches
                updatedDrive.setEligibleBranches(java.util.Arrays.asList(user.getAdminBranch()));
            }

            drive.setCompany(updatedDrive.getCompany());
            drive.setDate(updatedDrive.getDate());
            drive.setTime(updatedDrive.getTime());
            drive.setVenue(updatedDrive.getVenue());
            drive.setPositions(updatedDrive.getPositions());
            drive.setEligibility(updatedDrive.getEligibility());

            return ResponseEntity.ok(interviewDriveRepo.save(drive));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'COMPANY_ADMIN', 'DEPT_ADMIN')")
    public ResponseEntity<?> deleteDrive(@PathVariable Long id, java.security.Principal principal) {
        String username = principal.getName();
        com.abhi.authProject.model.Users user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return interviewDriveRepo.findById(id).map(drive -> {
            // Security Check
            if ("COMPANY_ADMIN".equals(user.getRole())) {
                if (!drive.getCompany().equals(user.getCompanyName())) {
                    return ResponseEntity.status(403).body("You are not authorized to delete this interview drive.");
                }
            } else if ("DEPT_ADMIN".equals(user.getRole())) {
                // DEPT_ADMIN can only delete drives for their branch
                if (drive.getEligibleBranches() == null ||
                        !drive.getEligibleBranches().contains(user.getAdminBranch())) {
                    return ResponseEntity.status(403).body("You are not authorized to delete this interview drive.");
                }
            }
            interviewDriveRepo.delete(drive);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }
}
