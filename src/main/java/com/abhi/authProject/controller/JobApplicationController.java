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
            @RequestParam("applicantEmail") String applicantEmail,
            @RequestParam("applicantPhone") String applicantPhone,
            @RequestParam(value = "applicantRollNo", required = false) String applicantRollNo,
            @RequestParam(value = "coverLetter", required = false) String coverLetter,
            @RequestParam("resume") MultipartFile resume) {
        if (resume.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resume file is required.");
        }

        JobApplicationRequest1 applicationRequest = new JobApplicationRequest1(
                jobId, jobTitle, companyName, applicantName, applicantEmail, applicantPhone,
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

    @GetMapping("/admin/job-applications")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<JobApplication>> getAllJobApplications() {
        List<JobApplication> applications = jobApplicationService.getAllJobApplications();
        return ResponseEntity.ok(applications);
    }

    @GetMapping("/admin/job-applications/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<JobApplication> getJobApplicationById(@PathVariable Long id) {
        return jobApplicationService.getJobApplicationById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job Application not found"));
    }

    @PutMapping("/admin/job-applications/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<JobApplication> updateJobApplicationStatus(
            @PathVariable Long id,
            @RequestParam ApplicationStatus status) {
        try {
            JobApplication updatedApplication = jobApplicationService.updateApplicationStatus(id, status);
            return ResponseEntity.ok(updatedApplication);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) { // Catching a general Exception is simpler and safer
            logger.error("Error updating application status or sending email: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to update status and send notification email.");
        }
    }

    @GetMapping("/job-applications/my")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<JobApplication>> getMyJobApplications(Principal principal) {
        String email = principal.getName(); // Assuming username is email
        List<JobApplication> myApplications = jobApplicationRepository.findByApplicantEmail(email);
        return ResponseEntity.ok(myApplications);
    }
}