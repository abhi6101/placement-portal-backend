package com.abhi.authProject.repo;

import com.abhi.authProject.model.ResumeFile;
import com.abhi.authProject.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ResumeFileRepository extends JpaRepository<ResumeFile, Long> {
    Optional<ResumeFile> findByUser(Users user);

    Optional<ResumeFile> findByFileName(String fileName);
}
