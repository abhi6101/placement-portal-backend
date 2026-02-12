package com.abhi.authProject.controller;

import com.abhi.authProject.model.ApplicationStatus;
import com.abhi.authProject.model.InterviewApplication;
import com.abhi.authProject.repo.InterviewApplicationRepo;
import com.abhi.authProject.service.EmailService;
import com.abhi.authProject.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
public class InterviewApplicationController {

    @Autowired
    private InterviewApplicationRepo interviewApplicationRepo;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private com.abhi.authProject.repo.UserRepo userRepo;

    @Value("${placement.portal.application.recipient-email:hack2hired.official@gmail.com}")
    private String recipientEmail;

    @PostMapping(value = "/interview-applications/apply", consumes = { "multipart/form-data" })
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> applyForInterview(
            @RequestParam("interviewDriveId") Long interviewDriveId,
            @RequestParam("companyName") String companyName,
            @RequestParam("interviewDate") String interviewDate,
            @RequestParam("applicantName") String applicantName,
            @RequestParam("applicantEmail") String applicantEmail,
            @RequestParam("applicantPhone") String applicantPhone,
            @RequestParam("resume") MultipartFile resume) {

        if (resume.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resume file is required.");
        }

        try {
            String resumePath = fileStorageService.saveResume(resume, applicantName);

            InterviewApplication application = new InterviewApplication(
                    interviewDriveId, companyName, interviewDate,
                    applicantName, applicantEmail, applicantPhone, resumePath);

            interviewApplicationRepo.save(application);

            // 1. Send confirmation email to student
            String emailBody = "Dear " + applicantName + ",\n\n" +
                    "Your application for the interview drive at " + companyName + " on " + interviewDate
                    + " has been received.\n" +
                    "We will review your profile and notify you about the status.\n\n" +
                    "Best Regards,\nPlacement Team";

            emailService.sendEmail(applicantEmail, "Interview Application Received - " + companyName, emailBody);

            // 2. Send notification to HR/Admin
            String adminSubject = "New Interview Application: " + companyName + " - " + applicantName;
            String adminBody = "<h3>New Interview Application Received</h3>" +
                    "<p><strong>Company:</strong> " + companyName + "</p>" +
                    "<p><strong>Interview Date:</strong> " + interviewDate + "</p>" +
                    "<p><strong>Applicant Name:</strong> " + applicantName + "</p>" +
                    "<p><strong>Applicant Email:</strong> " + applicantEmail + "</p>" +
                    "<p><strong>Applicant Phone:</strong> " + applicantPhone + "</p>";

            try {
                emailService.sendEmailWithLocalFile(recipientEmail, adminSubject, adminBody, resumePath);
            } catch (Exception e) {
                // Log but don't fail the request
                System.err.println("Failed to send Admin notification: " + e.getMessage());
            }

            return ResponseEntity.ok("Interview application submitted successfully.");

        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store resume.");
        }
    }

    @GetMapping("/admin/interview-applications")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'COMPANY_ADMIN', 'DEPT_ADMIN')")
    public ResponseEntity<List<InterviewApplication>> getAllInterviewApplications(java.security.Principal principal) {
        String username = principal.getName();
        com.abhi.authProject.model.Users user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // COMPANY_ADMIN: Only see applications for their company
        if ("COMPANY_ADMIN".equals(user.getRole())) {
            return ResponseEntity.ok(interviewApplicationRepo.findByCompanyName(user.getCompanyName()));
        }

        // DEPT_ADMIN: Only see applications from students in their branch
        if ("DEPT_ADMIN".equals(user.getRole())) {
            String adminBranch = user.getAdminBranch();
            if (adminBranch != null && !adminBranch.isEmpty()) {
                // Filter applications by student's branch
                List<InterviewApplication> allApplications = interviewApplicationRepo.findAll();
                return ResponseEntity.ok(
                        allApplications.stream()
                                .filter(app -> {
                                    // Get student's branch from their email
                                    com.abhi.authProject.model.Users student = userRepo
                                            .findByEmail(app.getApplicantEmail()).orElse(null);
                                    return student != null && adminBranch.equals(student.getBranch());
                                })
                                .collect(java.util.stream.Collectors.toList()));
            }
        }

        // SUPER_ADMIN/ADMIN: See all applications
        return ResponseEntity.ok(interviewApplicationRepo.findAll());
    }

    @PutMapping("/admin/interview-applications/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'COMPANY_ADMIN', 'DEPT_ADMIN')")
    public ResponseEntity<InterviewApplication> updateStatus(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> payload,
            java.security.Principal principal) {

        InterviewApplication application = interviewApplicationRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));

        if (payload == null || !payload.containsKey("status")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status is required");
        }

        String username = principal.getName();
        com.abhi.authProject.model.Users user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // COMPANY_ADMIN: Can only change status for their company's applications
        if ("COMPANY_ADMIN".equals(user.getRole()) && !application.getCompanyName().equals(user.getCompanyName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: Not your company's application");
        }

        // DEPT_ADMIN: Can only change status for applications from their branch
        // students
        if ("DEPT_ADMIN".equals(user.getRole())) {
            String adminBranch = user.getAdminBranch();
            com.abhi.authProject.model.Users student = userRepo.findByEmail(application.getApplicantEmail())
                    .orElse(null);

            if (student == null || !adminBranch.equals(student.getBranch())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Access Denied: Student not in your department (" + adminBranch + ")");
            }
        }

        try {
            ApplicationStatus status = ApplicationStatus.valueOf(payload.get("status"));
            application.setStatus(status);
            interviewApplicationRepo.save(application);

            // Email Notification
            emailService.sendStatusUpdateEmail(
                    application.getApplicantEmail(),
                    application.getApplicantName(),
                    "Interview at " + application.getCompanyName(),
                    application.getCompanyName(),
                    status.name());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status");
        }

        return ResponseEntity.ok(application);
    }

    @GetMapping("/interview-applications/my")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<InterviewApplication>> getMyInterviewApplications(java.security.Principal principal) {
        String username = principal.getName();
        com.abhi.authProject.model.Users user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));

        // Query using the USER's EMAIL, not their username
        return ResponseEntity.ok(interviewApplicationRepo.findByApplicantEmail(user.getEmail()));
    }

    // Delete ALL interview applications (SUPER_ADMIN only) - Use with caution!
    @DeleteMapping("/admin/interview-applications/all")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> deleteAllInterviewApplications() {
        long count = interviewApplicationRepo.count();
        interviewApplicationRepo.deleteAll();
        return ResponseEntity.ok("Deleted " + count + " interview applications successfully.");
    }
}
