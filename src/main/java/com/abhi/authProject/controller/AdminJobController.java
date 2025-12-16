package com.abhi.authProject.controller;

import com.abhi.authProject.model.JobDetails;
import com.abhi.authProject.repo.JobDetailsRepo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/admin/jobs")
// PreAuthorize removed here to allow fine-grained control or handled by
// SecurityConfig + logic below
public class AdminJobController {

    @Autowired
    private JobDetailsRepo jobRepository;

    @Autowired
    private com.abhi.authProject.service.EmailService emailService;

    @Autowired
    private com.abhi.authProject.repo.UserRepo userRepo;

    @GetMapping
    public ResponseEntity<List<JobDetails>> getAllJobs(java.security.Principal principal) {
        String username = principal.getName();
        com.abhi.authProject.model.Users user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if ("COMPANY_ADMIN".equals(user.getRole())) {
            return ResponseEntity.ok(jobRepository.findByCompany_name(user.getCompanyName()));
        }
        // SUPER_ADMIN or ADMIN sees all
        return ResponseEntity.ok(jobRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<?> createJob(@RequestBody JobDetails job, java.security.Principal principal) {
        String username = principal.getName();
        com.abhi.authProject.model.Users user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if ("COMPANY_ADMIN".equals(user.getRole())) {
            // Force company name to match the admin's company
            if (user.getCompanyName() == null || user.getCompanyName().isEmpty()) {
                return ResponseEntity.badRequest().body("Company Admin does not have an assigned company.");
            }
            job.setCompany_name(user.getCompanyName());
        }

        JobDetails savedJob = jobRepository.save(job);

        // Async: Send notifications to all students
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                // Fetch only users with role USER ideally, butfindAll is fine for MVP if small
                // Better to add findByRole in UserRepo later
                List<com.abhi.authProject.model.Users> students = userRepo.findAll().stream()
                        .filter(u -> "USER".equals(u.getRole()))
                        .toList();

                for (com.abhi.authProject.model.Users student : students) {
                    emailService.sendNewJobAlert(
                            student.getEmail(),
                            student.getUsername(),
                            savedJob.getTitle(),
                            savedJob.getCompany_name(),
                            String.valueOf(savedJob.getSalary()),
                            savedJob.getApply_link());
                }
            } catch (Exception e) {
                System.err.println("Error sending bulk job alerts: " + e.getMessage());
            }
        });

        return ResponseEntity.ok(savedJob);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateJob(@PathVariable int id, @RequestBody JobDetails updatedJob,
            java.security.Principal principal) {
        String username = principal.getName();
        com.abhi.authProject.model.Users user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Optional<JobDetails> optionalJob = jobRepository.findById(id);
        if (optionalJob.isPresent()) {
            JobDetails job = optionalJob.get();

            // Security Check: Company Admin can only edit their own jobs
            if ("COMPANY_ADMIN".equals(user.getRole())) {
                if (!job.getCompany_name().equals(user.getCompanyName())) {
                    return ResponseEntity.status(403).body("You are not authorized to update this job.");
                }
                // Ensure they can't change the company name
                updatedJob.setCompany_name(user.getCompanyName());
            }

            job.setTitle(updatedJob.getTitle());
            job.setDescription(updatedJob.getDescription());
            job.setCompany_name(updatedJob.getCompany_name());
            job.setApply_link(updatedJob.getApply_link());
            job.setLast_date(updatedJob.getLast_date());
            job.setSalary(updatedJob.getSalary());
            job.setInterview_details(updatedJob.getInterview_details()); // Added missing field update
            return ResponseEntity.ok(jobRepository.save(job));
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteJob(@PathVariable int id, java.security.Principal principal) {
        String username = principal.getName();
        com.abhi.authProject.model.Users user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Optional<JobDetails> optionalJob = jobRepository.findById(id);
        if (optionalJob.isPresent()) {
            JobDetails job = optionalJob.get();

            // Security Check
            if ("COMPANY_ADMIN".equals(user.getRole())) {
                if (!job.getCompany_name().equals(user.getCompanyName())) {
                    return ResponseEntity.status(403).body("You are not authorized to delete this job.");
                }
            }

            jobRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
