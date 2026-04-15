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
import org.springframework.transaction.annotation.Transactional;

@RestController
public class JobController {

    @Autowired
    private JobService jobService;

    @Autowired
    private UserRepo userRepo;

    @GetMapping("/jobs")
    @Transactional(readOnly = true)
    public List<JobDetails> getJobs() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        System.out.println("DEBUG: /jobs called. User: " + (auth != null ? auth.getName() : "null"));

        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            Optional<Users> userOpt = userRepo.findByUsername(auth.getName());
            if (userOpt.isPresent()) {
                Users user = userOpt.get();
                // If user is a student (USER role), filter jobs
                if ("USER".equals(user.getRole())) {
                    System.out.println("DEBUG: Student Profile - Branch: '" + user.getBranch() + "', Semester: "
                            + user.getSemester());

                    if (user.getBranch() != null && user.getSemester() != null) {
                        List<JobDetails> jobs = jobService.getEligibleJobs(user.getBranch(), user.getSemester());
                        System.out.println("DEBUG: Found " + jobs.size() + " eligible jobs.");
                        return jobs;
                    } else {
                        System.out.println("DEBUG: Branch/Semester not set for student. Showing all jobs.");
                    }
                } else {
                    System.out
                            .println("DEBUG: User is not a student (Role: " + user.getRole() + "). Showing all jobs.");
                }
            }
        }

        return jobService.getAllJobs();
    }

    // CORRECTED: Security annotation is now enabled.
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/jobs")
    @Transactional
    public JobDetails addJob(@RequestBody JobDetails jobDetails) {
        return jobService.addJob(jobDetails);
    }
}