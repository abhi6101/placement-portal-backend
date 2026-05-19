package com.abhi.authProject.controller;

import com.abhi.authProject.model.Paper;
import com.abhi.authProject.repo.PaperRepository;
import com.abhi.authProject.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
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

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private com.abhi.authProject.repo.UserRepo userRepo;

    @Autowired
    private com.abhi.authProject.repo.PaperViewLogRepository paperViewLogRepository;

    @GetMapping("/papers/download/{id}")
    public ResponseEntity<?> downloadPaper(@PathVariable Long id) {
        try {
            Paper paper = paperRepository.findById(id).orElseThrow(() -> new RuntimeException("Paper not found"));
            
            // Retrieve current authenticated user and log the view
            try {
                org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                    String username = auth.getName();
                    com.abhi.authProject.model.Users user = userRepo.findByComputerCodeOrUsername(username).orElse(null);
                    if (user != null) {
                        com.abhi.authProject.model.PaperViewLog viewLog = new com.abhi.authProject.model.PaperViewLog(
                            user.getUsername(),
                            user.getName(),
                            user.getComputerCode(),
                            paper.getId(),
                            paper.getTitle(),
                            paper.getSubject(),
                            paper.getBranch(),
                            paper.getSemester(),
                            paper.getYear()
                        );
                        paperViewLogRepository.save(viewLog);
                    }
                }
            } catch (Exception logEx) {
                System.err.println("Failed to write paper view log: " + logEx.getMessage());
            }

            String fileUrl = paper.getPdfUrl();

            if (fileUrl == null || !fileUrl.startsWith("http")) {
                return ResponseEntity.notFound().build();
            }

            boolean isGoogleDrive = fileUrl.contains("drive.google.com") || fileUrl.contains("docs.google.com");

            if (isGoogleDrive) {
                String fileId = null;
                
                // Format 1: /d/FILE_ID
                java.util.regex.Pattern pattern1 = java.util.regex.Pattern.compile("/d/([^/&?]+)");
                java.util.regex.Matcher matcher1 = pattern1.matcher(fileUrl);
                if (matcher1.find()) {
                    fileId = matcher1.group(1);
                } else {
                    // Format 2: ?id=FILE_ID or &id=FILE_ID
                    java.util.regex.Pattern pattern2 = java.util.regex.Pattern.compile("[?&]id=([^/&?]+)");
                    java.util.regex.Matcher matcher2 = pattern2.matcher(fileUrl);
                    if (matcher2.find()) {
                        fileId = matcher2.group(1);
                    }
                }

                if (fileId != null) {
                    try {
                        java.io.InputStream inputStream = fileStorageService.getFileStream(fileId);
                        InputStreamResource resource = new InputStreamResource(inputStream);

                        return ResponseEntity.ok()
                                .contentType(MediaType.APPLICATION_PDF)
                                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + paper.getTitle().replaceAll("[^a-zA-Z0-9]", "_") + ".pdf\"")
                                .body(resource);
                    } catch (Exception e) {
                        System.err.println("Secure Streaming Failed for File ID " + fileId + ": " + e.getMessage());
                        return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                                .body("Unable to stream secure document. Access Denied.");
                    }
                } else {
                    return ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST)
                            .body("Invalid Google Drive URL structure.");
                }
            }

            // Redirect the client to the Cloudinary URL directly (Fallback)
            return ResponseEntity.status(org.springframework.http.HttpStatus.FOUND)
                    .location(java.net.URI.create(fileUrl))
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}
