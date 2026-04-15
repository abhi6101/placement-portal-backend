package com.abhi.authProject.repo;

import com.abhi.authProject.model.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepo extends JpaRepository<Application, Long> {

    // Find all applications by a student
    List<Application> findByStudentId(int studentId);

    // Find all applications for an interview drive
    List<Application> findByInterviewDriveId(Long driveId);

    // Check if student already applied to an interview
    Optional<Application> findByStudentIdAndInterviewDriveId(int studentId, Long driveId);

    // Find applications by status
    List<Application> findByStatus(Application.ApplicationStatus status);
}
