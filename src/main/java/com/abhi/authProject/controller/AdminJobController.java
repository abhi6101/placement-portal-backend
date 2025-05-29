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
@PreAuthorize("hasRole('ADMIN')")
public class AdminJobController {

    @Autowired
    private JobDetailsRepo jobRepository;

    @GetMapping
    public ResponseEntity<List<JobDetails>> getAllJobs() {
        return ResponseEntity.ok(jobRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<JobDetails> createJob(@RequestBody JobDetails job) {
        return ResponseEntity.ok(jobRepository.save(job));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateJob(@PathVariable int id, @RequestBody JobDetails updatedJob) {
        Optional<JobDetails> optionalJob = jobRepository.findById(id);
        if (optionalJob.isPresent()) {
            JobDetails job = optionalJob.get();
            job.setTitle(updatedJob.getTitle());
            job.setDescription(updatedJob.getDescription());
            job.setCompany_name(updatedJob.getCompany_name());
            job.setApply_link(updatedJob.getApply_link());
            job.setLast_date(updatedJob.getLast_date());
            job.setSalary(updatedJob.getSalary());
            return ResponseEntity.ok(jobRepository.save(job));
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteJob(@PathVariable int id) {
        if (jobRepository.existsById(id)) {
            jobRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}

