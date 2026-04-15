package com.abhi.authProject.controller;

import com.abhi.authProject.model.Application;
import com.abhi.authProject.model.InterviewDrive;
import com.abhi.authProject.model.Users;
import com.abhi.authProject.repo.ApplicationRepo;
import com.abhi.authProject.repo.InterviewDriveRepo;
import com.abhi.authProject.repo.UserRepo;
import com.abhi.authProject.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping("/api/applications")
public class ApplicationController {

    @Autowired
    private ApplicationRepo applicationRepo;

    @Autowired
    private InterviewDriveRepo interviewDriveRepo;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private EmailService emailService;

    // Student: Submit application
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> submitApplication(@RequestBody Map<String, Object> payload, Authentication auth) {
        String username = auth.getName();
        Users student = userRepo.findByUsername(username).orElse(null);

        if (student == null) {
            return ResponseEntity.badRequest().body("User not found");
        }

        Long driveId = Long.valueOf(payload.get("interviewDriveId").toString());
        InterviewDrive drive = interviewDriveRepo.findById(driveId).orElse(null);

        if (drive == null) {
            return ResponseEntity.badRequest().body("Interview drive not found");
        }

        // Check if already applied
        if (applicationRepo.findByStudentIdAndInterviewDriveId(student.getId(), driveId).isPresent()) {
            return ResponseEntity.badRequest().body("You have already applied to this interview");
        }

        Application application = new Application();
        application.setInterviewDrive(drive);
        application.setStudent(student);
        application.setResumeUrl(payload.get("resumeUrl").toString());
        application.setCoverLetter(payload.get("coverLetter").toString());

        return ResponseEntity.ok(applicationRepo.save(application));
    }

    // Student: Get my applications
    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getMyApplications(Authentication auth) {
        String username = auth.getName();
        Users student = userRepo.findByUsername(username).orElse(null);

        if (student == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(applicationRepo.findByStudentId(student.getId()));
    }

    // Student: Withdraw application
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> withdrawApplication(@PathVariable Long id, Authentication auth) {
        String username = auth.getName();
        Users student = userRepo.findByUsername(username).orElse(null);

        Application app = applicationRepo.findById(id).orElse(null);
        if (app == null || app.getStudent().getId() != student.getId()) {
            return ResponseEntity.notFound().build();
        }

        applicationRepo.deleteById(id);
        return ResponseEntity.ok().build();
    }

    // Admin: Get all applications for an interview
    @GetMapping("/interview/{driveId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getApplicationsByInterview(@PathVariable Long driveId) {
        return ResponseEntity.ok(applicationRepo.findByInterviewDriveId(driveId));
    }

    // Admin: Update application status
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'COMPANY_ADMIN')")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        Application app = applicationRepo.findById(id).orElse(null);
        if (app == null) {
            return ResponseEntity.notFound().build();
        }

        String oldStatus = app.getStatus().toString();
        String statusStr = payload.get("status");
        Application.ApplicationStatus status = Application.ApplicationStatus.valueOf(statusStr);
        app.setStatus(status);
        Application saved = applicationRepo.save(app);

        // Send email based on new status (only if status changed)
        if (!oldStatus.equals(statusStr)) {
            try {
                String studentEmail = app.getStudent().getEmail();
                String studentName = app.getStudent().getUsername();
                InterviewDrive drive = app.getInterviewDrive();
                String company = drive.getCompany();
                String jobTitle = "Interview at " + company;
                String interviewDate = drive.getDate() != null ? drive.getDate().toString() : "TBA";
                String interviewLocation = drive.getVenue() != null ? drive.getVenue() : "TBA";

                switch (statusStr) {
                    case "SHORTLISTED":
                        emailService.sendShortlistedEmail(
                                studentEmail,
                                studentName,
                                jobTitle,
                                company,
                                interviewDate,
                                interviewLocation);
                        break;
                    case "SELECTED":
                        emailService.sendSelectedEmail(
                                studentEmail,
                                studentName,
                                jobTitle,
                                company);
                        break;
                    case "REJECTED":
                        emailService.sendRejectedEmail(
                                studentEmail,
                                studentName,
                                jobTitle,
                                company);
                        break;
                    // PENDING - no email sent
                }
            } catch (Exception e) {
                // Log error but don't fail the request
                System.err.println("Failed to send status email: " + e.getMessage());
                e.printStackTrace(); // Print stack trace for debugging
            }
        }

        return ResponseEntity.ok(saved);
    }

    // Admin: Get all applications
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllApplications() {
        return ResponseEntity.ok(applicationRepo.findAll());
    }
}
