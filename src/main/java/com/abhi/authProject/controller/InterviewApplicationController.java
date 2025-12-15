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
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<InterviewApplication>> getAllInterviewApplications() {
        return ResponseEntity.ok(interviewApplicationRepo.findAll());
    }

    @PutMapping("/admin/interview-applications/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InterviewApplication> updateStatus(
            @PathVariable Long id,
            @RequestParam ApplicationStatus status) {

        InterviewApplication application = interviewApplicationRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));

        application.setStatus(status);
        interviewApplicationRepo.save(application);

        // Send notification email
        String subject = "Update on your Interview Application - " + application.getCompanyName();
        String body = "";

        switch (status) {
            case SHORTLISTED:
                body = "Dear " + application.getApplicantName() + ",\n\n" +
                        "Congratulations! You have been SHORTLISTED for the interview drive at "
                        + application.getCompanyName() + ".\n" +
                        "Please report to the venue on time.\n\n" +
                        "Best Regards,\nPlacement Team";
                break;
            case REJECTED:
                body = "Dear " + application.getApplicantName() + ",\n\n" +
                        "We regret to inform you that your application for " + application.getCompanyName()
                        + " was not selected.\n" +
                        "We wish you the best for future opportunities.\n\n" +
                        "Best Regards,\nPlacement Team";
                break;
            case SELECTED:
                body = "Dear " + application.getApplicantName() + ",\n\n" +
                        "Congratulations! You have been SELECTED by " + application.getCompanyName() + ".\n" +
                        "Further details will be shared shortly.\n\n" +
                        "Best Regards,\nPlacement Team";
                break;
            default:
                body = "Dear " + application.getApplicantName() + ",\n\n" +
                        "Your application status for " + application.getCompanyName() + " has been updated to: "
                        + status + ".\n\n" +
                        "Best Regards,\nPlacement Team";
        }

        try {
            emailService.sendEmail(application.getApplicantEmail(), subject, body);
        } catch (IOException e) {
            // Log error but don't fail the request since status is already updated
            e.printStackTrace();
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
