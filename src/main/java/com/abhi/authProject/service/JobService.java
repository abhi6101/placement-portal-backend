package com.abhi.authProject.service;

import com.abhi.authProject.model.JobDetails;
import com.abhi.authProject.repo.JobDetailsRepo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JobService {

    private final JobDetailsRepo repo;

    public JobService(JobDetailsRepo repo) {
        this.repo = repo;
    }

    public List<JobDetails> getAllJobs() {
        return repo.findAll();
    }

    public JobDetails addJob(JobDetails jobDetails) {
        return repo.save(jobDetails);
    }
}
