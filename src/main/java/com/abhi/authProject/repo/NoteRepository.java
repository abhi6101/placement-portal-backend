package com.abhi.authProject.repo;

import com.abhi.authProject.model.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {
    
    // Find all notes sorted by upload date descending
    List<Note> findAllByOrderByUploadedAtDesc();
    
    // Find notes by specific visibility level
    List<Note> findByVisibilityOrderByUploadedAtDesc(String visibility);
}
