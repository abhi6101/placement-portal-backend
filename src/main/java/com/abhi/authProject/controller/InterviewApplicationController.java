package com.abhi.authProject.controller;

import com.abhi.authProject.model.ApplicationStatus;
import com.abhi.authProject.model.InterviewApplication;
import com.abhi.authProject.repo.InterviewApplicationRepo;
import com.abhi.authProject.service.EmailService;
import com.abhi.authProject.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
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

            // Send confirmation email to student
            String emailBody = "Dear " + applicantName + ",\n\n" +
                    "Your application for the interview drive at " + companyName + " on " + interviewDate
                    + " has been received.\n" +
                    "We will review your profile and notify you about the status.\n\n" +
                    "Best Regards,\nPlacement Team";

            emailService.sendEmail(applicantEmail, "Interview Application Received - " + companyName, emailBody);

            return ResponseEntity.ok("Interview application submitted successfully.");

        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store resume.");
        }
    }

    @GetMapping("/admin/interview-applications")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'COMPANY_ADMIN')")
    public ResponseEntity<List<InterviewApplication>> getAllInterviewApplications(java.security.Principal principal) {
        String username = principal.getName();
        com.abhi.authProject.model.Users user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if ("COMPANY_ADMIN".equals(user.getRole())) {
            return ResponseEntity.ok(interviewApplicationRepo.findByCompanyName(user.getCompanyName()));
        }

        return ResponseEntity.ok(interviewApplicationRepo.findAll());
    }

    @PutMapping("/admin/interview-applications/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'COMPANY_ADMIN')")
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

        if ("COMPANY_ADMIN".equals(user.getRole()) && !application.getCompanyName().equals(user.getCompanyName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied");
        }

        try {
            ApplicationStatus status = ApplicationStatus.valueOf(payload.get("status"));
            application.setStatus(status);
            interviewApplicationRepo.save(application);

            // Use the standardized email service
            // Note: 'Interview Title' isn't directly available in InterviewApplication,
            // usually we'd fetch the Drive.
            // But for now we pass "Interview at " + companyName
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
        String email = principal.getName();
        return ResponseEntity.ok(interviewApplicationRepo.findByApplicantEmail(email));
    }
}
