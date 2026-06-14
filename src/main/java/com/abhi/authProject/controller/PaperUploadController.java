package com.abhi.authProject.controller;

import com.abhi.authProject.model.Paper;
import com.abhi.authProject.model.StudentPaper;
import com.abhi.authProject.model.Users;
import com.abhi.authProject.repo.PaperRepository;
import com.abhi.authProject.repo.StudentPaperRepository;
import com.abhi.authProject.repo.UserRepo;
import com.abhi.authProject.service.FileStorageService;
import com.abhi.authProject.service.PdfCompilationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/papers")
@CrossOrigin("*")
public class PaperUploadController {

    @Autowired
    private PdfCompilationService pdfCompilationService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private StudentPaperRepository studentPaperRepository;

    @Autowired
    private PaperRepository paperRepository;

    @Autowired
    private UserRepo userRepo;



    /**
     * Step 5: Submit verified data and compile PDF
     */
    @PostMapping("/submit")
    public ResponseEntity<?> submitPaper(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("subject") String subject,
            @RequestParam("semester") Integer semester,
            @RequestParam("branch") String branch,
            @RequestParam("year") String year) {

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            Users uploader = userRepo.findByUsername(username).orElse(null);

            if (uploader == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"message\": \"User not found\"}");
            }

            // Compile PDF
            byte[] compiledPdf = pdfCompilationService.compileImagesToPdf(files, uploader.getName() != null ? uploader.getName() : uploader.getUsername());

            // Upload compiledPdf to Google Drive
            String fileName = String.format("%s_%s_Sem%s_%s.pdf", subject, branch, semester, year).replaceAll(" ", "_");
            java.io.InputStream is = new java.io.ByteArrayInputStream(compiledPdf);
            String driveFileId = fileStorageService.saveFileFromStream(is, fileName, "");

            // Save to Database as PENDING
            StudentPaper paper = new StudentPaper();
            paper.setSubject(subject);
            paper.setSemester(semester);
            paper.setBranch(branch);
            paper.setYear(year);
            paper.setStatus("PENDING");
            paper.setUploadedBy(uploader);
            paper.setDriveFileId(driveFileId);
            
            studentPaperRepository.save(paper);

            return ResponseEntity.ok("Paper submitted successfully and is pending admin approval.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"message\": \"Error submitting paper: " + e.getMessage().replace("\"", "\\\"") + "\"}");
        }
    }

    /**
     * Admin: Get all pending papers
     */
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingPapers() {
        try {
            List<StudentPaper> pendingPapers = studentPaperRepository.findByStatus("PENDING");
            return ResponseEntity.ok(pendingPapers);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error fetching pending papers.");
        }
    }

    /**
     * Admin: Securely view a pending paper
     */
    @GetMapping("/pending/{id}/view")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'COMPANY_ADMIN', 'DEPT_ADMIN')")
    public ResponseEntity<?> viewPendingPaper(@PathVariable Long id) {
        try {
            Optional<StudentPaper> optionalPaper = studentPaperRepository.findById(id);
            if (!optionalPaper.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Paper not found");
            }
            StudentPaper paper = optionalPaper.get();
            String fileUrl = paper.getDriveFileId();
            
            if (fileUrl == null) {
                return ResponseEntity.notFound().build();
            }

            String fileId = null;
            if (fileUrl.contains("/d/")) {
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("/d/([^/&?]+)");
                java.util.regex.Matcher matcher = pattern.matcher(fileUrl);
                if (matcher.find()) {
                    fileId = matcher.group(1);
                }
            } else if (fileUrl.contains("id=")) {
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("id=([^/&?]+)");
                java.util.regex.Matcher matcher = pattern.matcher(fileUrl);
                if (matcher.find()) {
                    fileId = matcher.group(1);
                }
            } else if (!fileUrl.contains("/") && !fileUrl.contains("http")) {
                fileId = fileUrl;
            }


            if (fileId != null) {
                java.io.InputStream inputStream = fileStorageService.getFileStream(fileId);
                org.springframework.core.io.InputStreamResource resource = new org.springframework.core.io.InputStreamResource(inputStream);

                return ResponseEntity.ok()
                        .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                        .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"pending_paper_" + id + ".pdf\"")
                        .body(resource);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid Google Drive URL structure.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error streaming paper.");
        }
    }

    /**
     * Admin: Approve a paper and award points
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approvePaper(@PathVariable Long id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String adminUsername = authentication.getName();
            Users admin = userRepo.findByUsername(adminUsername).orElse(null);

            Optional<StudentPaper> optionalPaper = studentPaperRepository.findById(id);
            if (!optionalPaper.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Paper not found");
            }

            StudentPaper paper = optionalPaper.get();
            
            if ("APPROVED".equals(paper.getStatus())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Paper is already approved.");
            }

            paper.setStatus("APPROVED");
            paper.setApprovedBy(admin);
            studentPaperRepository.save(paper);

            // Award points to the uploader
            Users uploader = paper.getUploadedBy();
            if (uploader != null) {
                uploader.setContributionPoints(uploader.getContributionPoints() + 50);
                userRepo.save(uploader);
            }

            // Convert to a public Paper
            try {
                int extractedYear = 2025; // Default fallback
                if (paper.getYear() != null) {
                    String yearStr = paper.getYear().replaceAll("[^0-9]", "");
                    if (!yearStr.isEmpty()) {
                        extractedYear = Integer.parseInt(yearStr);
                    }
                }
                String fullYearStr = paper.getYear() != null ? paper.getYear() : String.valueOf(extractedYear);
                String title = paper.getSubject() + " " + fullYearStr + " Exam";
                Paper publicPaper = new Paper(
                        title,
                        paper.getSubject(),
                        extractedYear,
                        paper.getSemester(),
                        paper.getBranch(),
                        "University",
                        "End-Sem", // Default
                        "DAVV", // Default
                        paper.getDriveFileId()
                );
                paperRepository.save(publicPaper);
            } catch (Exception ex) {
                System.err.println("Failed to create public paper: " + ex.getMessage());
            }

            return ResponseEntity.ok("Paper approved, converted to public, and 50 points awarded to " + (uploader != null ? uploader.getUsername() : "unknown user"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error approving paper.");
        }
    }

    /**
     * Admin: Reject a paper
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<?> rejectPaper(@PathVariable Long id, @RequestParam("reason") String reason) {
        try {
            Optional<StudentPaper> optionalPaper = studentPaperRepository.findById(id);
            if (!optionalPaper.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Paper not found");
            }

            StudentPaper paper = optionalPaper.get();
            
            if ("REJECTED".equals(paper.getStatus())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Paper is already rejected.");
            }

            boolean wasApproved = "APPROVED".equals(paper.getStatus());

            paper.setStatus("REJECTED");
            paper.setRejectionReason(reason);
            studentPaperRepository.save(paper);

            // If it was previously approved, we must deduct the 50 points we gave them
            if (wasApproved) {
                Users uploader = paper.getUploadedBy();
                if (uploader != null) {
                    int newPoints = Math.max(0, uploader.getContributionPoints() - 50);
                    uploader.setContributionPoints(newPoints);
                    userRepo.save(uploader);
                }
            }

            return ResponseEntity.ok("Paper rejected successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error rejecting paper.");
        }
    }
}
