package com.abhi.authProject.repo;

import com.abhi.authProject.model.JobDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobDetailsRepo extends JpaRepository<JobDetails, Integer> {

    // Custom query to find jobs by company name (snake_case field)
    @org.springframework.data.jpa.repository.Query("SELECT j FROM JobDetails j WHERE j.company_name = :companyName")
    java.util.List<JobDetails> findByCompany_name(
            @org.springframework.web.bind.annotation.RequestParam("companyName") String companyName);
}
