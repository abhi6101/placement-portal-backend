package com.abhi.authProject.controller;

import com.abhi.authProject.model.JobDetails;
import com.abhi.authProject.model.Users;
import com.abhi.authProject.repo.UserRepo;
import com.abhi.authProject.service.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
public class JobController {

    @Autowired
    private JobService jobService;

    @Autowired
    private UserRepo userRepo;

    @GetMapping("/jobs")
    public List<JobDetails> getJobs() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            Optional<Users> userOpt = userRepo.findByUsername(auth.getName());
            if (userOpt.isPresent()) {
                Users user = userOpt.get();
                // If user is a student (USER role), filter jobs
                if ("USER".equals(user.getRole())) {
                    // Start debugging logs if needed
                    // System.out.println("Filtering jobs for: " + user.getUsername() + " Branch: "
                    // + user.getBranch() + " Sem: " + user.getSemester());

                    if (user.getBranch() != null && user.getSemester() != null) {
                        return jobService.getEligibleJobs(user.getBranch(), user.getSemester());
                    }
                }
            }
        }

        // Default: return all jobs (for Admin, Company Admin, or if user details
        // missing)
        return jobService.getAllJobs();
    }

    // CORRECTED: Security annotation is now enabled.
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/jobs")
    public JobDetails addJob(@RequestBody JobDetails jobDetails) {
        return jobService.addJob(jobDetails);
    }
}