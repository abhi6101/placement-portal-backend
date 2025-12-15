package com.abhi.authProject.repo;

import com.abhi.authProject.model.JobApplication;
import com.abhi.authProject.model.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {
    // Custom queries if needed, e.g., find by applicant email
    Optional<JobApplication> findByApplicantEmailAndJobId(String email, String jobId);

    List<JobApplication> findByStatus(ApplicationStatus status);

    List<JobApplication> findByApplicantEmail(String email);
}