package com.abhi.authProject.repo;

import com.abhi.authProject.model.JobDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobDetailsRepo extends JpaRepository <JobDetails,Integer> {
  
}

