package com.abhi.authProject.repo;

import com.abhi.authProject.model.JobDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JobDetailsRepo extends JpaRepository<JobDetails, Integer> {

    // Custom query to find jobs by company name (snake_case field)
    @Query("SELECT j FROM JobDetails j WHERE j.company_name = :companyName")
    java.util.List<JobDetails> findByCompany_name(
            @org.springframework.web.bind.annotation.RequestParam("companyName") String companyName);

    @Query("SELECT COUNT(j) FROM JobDetails j WHERE j.company_name = :companyName")
    long countByCompany_name(@org.springframework.web.bind.annotation.RequestParam("companyName") String companyName);

    // Find jobs eligible for a student based on branch and semester
    // If eligibleBranches or eligibleSemesters is null/empty, job is visible to all
    @Query("SELECT j FROM JobDetails j WHERE " +
            "(j.eligibleBranches IS EMPTY OR :branch MEMBER OF j.eligibleBranches) AND " +
            "(j.eligibleSemesters IS EMPTY OR :semester MEMBER OF j.eligibleSemesters)")
    java.util.List<JobDetails> findEligibleJobs(
            @Param("branch") String branch,
            @Param("semester") Integer semester);
}
