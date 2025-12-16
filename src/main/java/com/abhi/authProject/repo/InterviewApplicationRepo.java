package com.abhi.authProject.repo;

import com.abhi.authProject.model.InterviewApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InterviewApplicationRepo extends JpaRepository<InterviewApplication, Long> {
    List<InterviewApplication> findByApplicantEmail(String applicantEmail);

    List<InterviewApplication> findByInterviewDriveId(Long interviewDriveId);

    List<InterviewApplication> findByCompanyName(String companyName);
}
