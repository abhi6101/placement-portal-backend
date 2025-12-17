package com.abhi.authProject.controller;

import com.abhi.authProject.model.ApplicationStatus;
import com.abhi.authProject.model.JobApplication;
import com.abhi.authProject.model.JobApplicationRequest1;
import com.abhi.authProject.repo.JobApplicationRepository;
import com.abhi.authProject.service.JobApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException; // Keep this import
import java.security.Principal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class JobApplicationController {

    private static final Logger logger = LoggerFactory.getLogger(JobApplicationController.class);
    private final JobApplicationService jobApplicationService;
    private final JobApplicationRepository jobApplicationRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private com.abhi.authProject.repo.UserRepo userRepo;

    public JobApplicationController(JobApplicationService jobApplicationService,
            JobApplicationRepository jobApplicationRepository) {
        this.jobApplicationService = jobApplicationService;
        this.jobApplicationRepository = jobApplicationRepository;
    }

    @PostMapping(value = "/apply-job", consumes = { "multipart/form-data" })
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> applyForJob(
            @RequestParam("jobId") String jobId,
            @RequestParam("jobTitle") String jobTitle,
            @RequestParam("companyName") String companyName,
            @RequestParam("applicantName") String applicantName,
            @RequestParam("applicantEmail") String applicantEmail, // Kept to match frontend request
            @RequestParam("applicantPhone") String applicantPhone,
            @RequestParam(value = "applicantRollNo", required = false) String applicantRollNo,
            @RequestParam(value = "coverLetter", required = false) String coverLetter,
            @RequestParam("resume") MultipartFile resume,
            Principal principal) { // Added Principal
        if (resume.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resume file is required.");
        }

        // Use the authenticated user's actual email from the database
        String username = principal.getName();
        com.abhi.authProject.model.Users user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        String authenticatedEmail = user.getEmail();

        JobApplicationRequest1 applicationRequest = new JobApplicationRequest1(
                jobId, jobTitle, companyName, applicantName, authenticatedEmail, applicantPhone,
                applicantRollNo, coverLetter, resume);

        try {
            jobApplicationService.processJobApplication(applicationRequest);
            return ResponseEntity.ok("Job application submitted successfully. Confirmation email sent.");
        } catch (IOException e) {
            logger.error("Error processing job application (saving file or sending email): {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to process resume or send confirmation email.");
        } catch (Exception e) {
            logger.error("An unexpected error occurred during application processing: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
        }
    }

    @GetMapping("/job-applications/my")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<JobApplication>> getMyJobApplications(Principal principal) {
        String email = principal.getName();
        List<JobApplication> applications = jobApplicationRepository.findByApplicantEmail(email);
        return ResponseEntity.ok(applications);
    }

    @GetMapping("/admin/job-applications")
    // Allow any admin role (handled by security config usually, but permissive
    // here)
    public ResponseEntity<List<JobApplication>> getAllJobApplications(Principal principal) {
        String username = principal.getName();
        com.abhi.authProject.model.Users user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if ("COMPANY_ADMIN".equals(user.getRole())) {
            return ResponseEntity.ok(jobApplicationRepository.findByCompanyName(user.getCompanyName()));
        }

        List<JobApplication> applications = jobApplicationService.getAllJobApplications();
        return ResponseEntity.ok(applications);
    }

    @GetMapping("/admin/job-applications/{id}")
    public ResponseEntity<JobApplication> getJobApplicationById(@PathVariable Long id, Principal principal) {
        JobApplication app = jobApplicationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job Application not found"));

        // Security Check
        String username = principal.getName();
        com.abhi.authProject.model.Users user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if ("COMPANY_ADMIN".equals(user.getRole()) && !app.getCompanyName().equals(user.getCompanyName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied");
        }

        return ResponseEntity.ok(app);
    }

    @PutMapping("/admin/job-applications/{id}/status")
    public ResponseEntity<JobApplication> updateJobApplicationStatus(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> payload,
            Principal principal) {

        // Security Check First
        String username = principal.getName();
        com.abhi.authProject.model.Users user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        JobApplication app = jobApplicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("App not found"));

        if ("COMPANY_ADMIN".equals(user.getRole()) && !app.getCompanyName().equals(user.getCompanyName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied");
        }

        try {
            ApplicationStatus status = ApplicationStatus.valueOf(payload.get("status"));
            JobApplication updatedApplication = jobApplicationService.updateApplicationStatus(id, status);
            return ResponseEntity.ok(updatedApplication);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status or ID: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error updating application status or sending email: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to update status and send notification email.");
        }
    }

    @DeleteMapping("/admin/job-applications/{id}")
    public ResponseEntity<Void> deleteJobApplication(@PathVariable Long id, Principal principal) {
        JobApplication app = jobApplicationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job Application not found"));

        // Security Check
        String username = principal.getName();
        com.abhi.authProject.model.Users user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if ("COMPANY_ADMIN".equals(user.getRole()) && !app.getCompanyName().equals(user.getCompanyName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied");
        }

        jobApplicationRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

}