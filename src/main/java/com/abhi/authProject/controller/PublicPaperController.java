package com.abhi.authProject.controller;

import com.abhi.authProject.model.Paper;
import com.abhi.authProject.repo.PaperRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class PublicPaperController {

    @Autowired
    private PaperRepository paperRepository;

    @GetMapping("/papers/download/{id}")
    public ResponseEntity<Void> downloadPaper(@PathVariable Long id) {
        try {
            Paper paper = paperRepository.findById(id).orElseThrow(() -> new RuntimeException("Paper not found"));
            String fileUrl = paper.getDownloadUrl();
            if (fileUrl == null)
                fileUrl = paper.getPdfUrl();

            if (fileUrl == null || !fileUrl.startsWith("http")) {
                return ResponseEntity.notFound().build();
            }

            // Redirect the client to the Cloudinary URL directly
            // This solves CORS (since it's a navigation) and Load (server doesn't proxy
            // bytes)
            return ResponseEntity.status(org.springframework.http.HttpStatus.FOUND)
                    .location(java.net.URI.create(fileUrl))
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}
