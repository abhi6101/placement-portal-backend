package com.abhi.authProject.repo;


import com.abhi.authProject.model.Interview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewRepository extends JpaRepository<Interview, Long> {
    Optional<Interview> findByJobApplication_Id(Long jobApplicationId);
    List<Interview> findByJobApplication_ApplicantEmail(String applicantEmail);
}