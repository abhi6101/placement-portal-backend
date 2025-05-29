package com.abhi.authProject.repo; // Consistent with your UserRepo package

import com.abhi.authProject.model.Paper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository // Marks this interface as a Spring Data repository
public interface PaperRepository extends JpaRepository<Paper, Long> { // Paper is the entity, Long is the ID type

    // Spring Data automatically generates the SQL query for this method:
    // It fetches all papers and orders them by 'uploadedAt' in descending order (newest first).
    List<Paper> findAllByOrderByUploadedAtDesc();

    // You can add more custom query methods here if needed, for example:
    // List<Paper> findBySubject(String subject);
    // List<Paper> findByYear(int year);
    // List<Paper> findByTitleContainingIgnoreCase(String title);
}