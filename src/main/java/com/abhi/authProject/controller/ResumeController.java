package com.abhi.authProject.controller;

import com.abhi.authProject.model.ResumeData;
import com.abhi.authProject.service.ResumePdfService;
import jakarta.validation.Valid;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/resume")
// Removed @CrossOrigin annotation from here, as it's now handled globally in SecurityConfig
public class ResumeController {

    private final ResumePdfService resumePdfService;

    public ResumeController(ResumePdfService resumePdfService) {
        this.resumePdfService = resumePdfService;
    }

    @PostMapping("/generate-pdf")
    public ResponseEntity<?> generatePdf(@Valid @RequestBody ResumeData resumeData) {
        try {
            String uniqueFileName = resumePdfService.generateResumePdf(resumeData);
            // Updated downloadUrl to use the deployed Render backend URL
            String downloadUrl = "https://placement-portal-backend-nwaj.onrender.com/api/resume/download/" + uniqueFileName;
            return ResponseEntity.ok(Map.of("downloadUrl", downloadUrl));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Error generating resume: " + e.getMessage()));
        }
    }

    @GetMapping("/download/{filename}")
    public ResponseEntity<?> downloadResume(@PathVariable String filename) {
        try {
            ByteArrayResource resource = resumePdfService.loadPdfAsResource(filename);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
            headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
            headers.add(HttpHeaders.PRAGMA, "no-cache");
            headers.add(HttpHeaders.EXPIRES, "0");

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(resource.contentLength())
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.status(404)
                    .body("File not found: " + filename);
        }
    }
}
