package com.abhi.authProject.repo;

import com.abhi.authProject.model.StudentProfile;
import com.abhi.authProject.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentProfileRepo extends JpaRepository<StudentProfile, Long> {
    Optional<StudentProfile> findByUser(Users user);

    Optional<StudentProfile> findByUserId(int userId);

    java.util.List<StudentProfile> findByBranch(String branch);
}
