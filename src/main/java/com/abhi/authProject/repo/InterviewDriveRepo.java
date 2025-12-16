package com.abhi.authProject.repo;

import com.abhi.authProject.model.InterviewDrive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface InterviewDriveRepo extends JpaRepository<InterviewDrive, Long> {
    List<InterviewDrive> findByDateAfterOrderByDateAsc(LocalDate date);

    long countByCompany(String company);
}
