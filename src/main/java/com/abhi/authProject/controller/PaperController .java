package com.abhi.authProject.controller;

import com.abhi.authProject.model.Paper;
import com.abhi.authProject.repo.PaperRepository; // Use the correct repo package
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // For optional role-based access control
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController // Marks this class as a RESTful web service controller
@RequestMapping("/api") // All endpoints in this controller will start with /api
// @CrossOrigin(...) // IMPORTANT: If you manage CORS globally in your SecurityConfig, REMOVE or COMMENT OUT this @CrossOrigin annotation.
public class PaperController {

    private final PaperRepository paperRepository; // Using constructor injection for dependency

    @Autowired // Spring automatically provides an instance of PaperRepository
    public PaperController(PaperRepository paperRepository) {
        this.paperRepository = paperRepository;
    }

    /**
     * Handles GET requests to /api/papers.
     * Fetches all previous year papers from the database, sorted by upload date (newest first).
     *
     * @return A ResponseEntity containing a list of Paper objects.
     */
    @GetMapping("/papers")
    // Uncomment the @PreAuthorize below if you want only authenticated users (students/admins) to see the papers.
    // If papers should be publicly accessible to anyone, keep @PreAuthorize commented out.
    // Make sure this matches the rule in SecurityConfig.java for /api/papers.
    // @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<Paper>> getAllPapers() {
        List<Paper> papers = paperRepository.findAllByOrderByUploadedAtDesc();
        return ResponseEntity.ok(papers); // Return 200 OK with the list of papers
    }
}