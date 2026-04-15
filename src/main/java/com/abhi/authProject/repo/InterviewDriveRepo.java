package com.abhi.authProject.repo;

import com.abhi.authProject.model.InterviewDrive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface InterviewDriveRepo extends JpaRepository<InterviewDrive, Long> {
    List<InterviewDrive> findByDateAfterOrderByDateAsc(LocalDate date);

    long countByCompany(String company);

    // Find interviews eligible for a student based on branch and semester
    // If eligibleBranches or eligibleSemesters is null/empty, interview is visible
    // to all
    @Query("SELECT i FROM InterviewDrive i WHERE " +
            "(i.eligibleBranches IS EMPTY OR :branch MEMBER OF i.eligibleBranches) AND " +
            "(i.eligibleSemesters IS EMPTY OR :semester MEMBER OF i.eligibleSemesters)")
    List<InterviewDrive> findEligibleInterviews(
            @Param("branch") String branch,
            @Param("semester") Integer semester);
}
