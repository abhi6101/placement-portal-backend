package com.abhi.authProject.repo;

import com.abhi.authProject.model.StudentPaper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentPaperRepository extends JpaRepository<StudentPaper, Long> {
    
    List<StudentPaper> findByStatus(String status);
    
    List<StudentPaper> findByUploadedById(int userId);
    
    List<StudentPaper> findByApprovedById(int userId);
    
    List<StudentPaper> findByBranchAndSemesterAndStatus(String branch, Integer semester, String status);
}
