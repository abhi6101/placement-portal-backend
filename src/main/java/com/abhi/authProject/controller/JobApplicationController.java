package com.abhi.authProject.controller;

import com.abhi.authProject.model.JobApplicationRequest1;
import com.abhi.authProject.model.ApplicationStatus; // New import
import com.abhi.authProject.model.JobApplication; // New import
import com.abhi.authProject.service.JobApplicationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import jakarta.mail.MessagingException;
import java.io.IOException;
import java.util.List;
import java.util.Optional;


@RestController
@RequestMapping("/api")
// Remove @CrossOrigin here if you manage CORS globally in SecurityConfig
// @CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500"}, allowCredentials = "true")
public class JobApplicationController {

    private final JobApplicationService jobApplicationService;

    public JobApplicationController(JobApplicationService jobApplicationService) {
        this.jobApplicationService = jobApplicationService;
    }

    // Endpoint for Student to apply
    @PostMapping(value = "/apply-job", consumes = {"multipart/form-data"})
    @PreAuthorize("hasRole('USER')") // Only authenticated users can apply
    public ResponseEntity<String> applyForJob(
            @RequestParam("jobId") String jobId,
            @RequestParam("jobTitle") String jobTitle,
            @RequestParam("companyName") String companyName,
            @RequestParam("applicantName") String applicantName,
            @RequestParam("applicantEmail") String applicantEmail,
            @RequestParam("applicantPhone") String applicantPhone,
            @RequestParam(value = "applicantRollNo", required = false) String applicantRollNo,
            @RequestParam(value = "coverLetter", required = false) String coverLetter,
            @RequestParam("resume") MultipartFile resume
    ) {
        if (resume.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resume file is required.");
        }

        JobApplicationRequest1 applicationRequest = new JobApplicationRequest1(
                jobId, jobTitle, companyName, applicantName, applicantEmail, applicantPhone,
                applicantRollNo, coverLetter, resume
        );

        try {
            jobApplicationService.processJobApplication(applicationRequest);
            return ResponseEntity.ok("Job application submitted successfully. Confirmation email sent.");
        } catch (MessagingException e) {
            System.err.println("Error sending email: " + e.getMessage());
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to send application email: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error saving resume or processing job application: " + e.getMessage());
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to process resume: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error processing job application: " + e.getMessage());
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred during application processing: " + e.getMessage());
        }
    }

    // New Endpoints for HR/Admin to manage applications

    @GetMapping("/admin/job-applications")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<JobApplication>> getAllJobApplications() {
        List<JobApplication> applications = jobApplicationService.getAllJobApplications();
        return ResponseEntity.ok(applications);
    }

    @GetMapping("/admin/job-applications/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<JobApplication> getJobApplicationById(@PathVariable Long id) {
        Optional<JobApplication> application = jobApplicationService.getJobApplicationById(id);
        return application.map(ResponseEntity::ok)
                          .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job Application not found"));
    }

    @PutMapping("/admin/job-applications/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<JobApplication> updateJobApplicationStatus(
            @PathVariable Long id,
            @RequestParam ApplicationStatus status // This will map to the enum
    ) {
        try {
            JobApplication updatedApplication = jobApplicationService.updateApplicationStatus(id, status);
            return ResponseEntity.ok(updatedApplication);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (MessagingException | IOException e) {
            System.err.println("Error updating application status or sending email: " + e.getMessage());
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update status and send email: " + e.getMessage());
        }
    }
}
