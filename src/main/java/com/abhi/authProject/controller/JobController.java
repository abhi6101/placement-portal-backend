package com.abhi.authProject.controller;

import com.abhi.authProject.model.JobDetails;
import com.abhi.authProject.service.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class JobController {

    @Autowired
    private JobService jobService;

//    @PreAuthorize("hasRole('USER')")
    @GetMapping("/jobs")
    public List<JobDetails> getJobs(){
        return jobService.getAllJobs();
    }

//    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/jobs")
    public JobDetails addJob(@RequestBody JobDetails jobDetails){
        return jobService.addJob(jobDetails);
    }
}
