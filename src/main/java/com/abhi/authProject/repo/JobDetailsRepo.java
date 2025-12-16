package com.abhi.authProject.repo;

import com.abhi.authProject.model.JobDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobDetailsRepo extends JpaRepository<JobDetails, Integer> {

    // Custom query to find jobs by company name (snake_case field)
    java.util.List<JobDetails> findByCompany_name(String company_name);
}
