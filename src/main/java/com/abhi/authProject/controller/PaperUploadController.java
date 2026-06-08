package com.abhi.authProject.controller;

import com.abhi.authProject.model.StudentPaper;
import com.abhi.authProject.model.Users;
import com.abhi.authProject.repo.StudentPaperRepository;
import com.abhi.authProject.repo.UserRepo;
import com.abhi.authProject.service.PdfCompilationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private StudentPaperRepository studentPaperRepository;

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

            // TODO: Here you would upload compiledPdf to Google Drive and get the driveFileId
            String driveFileId = "MOCKED_DRIVE_ID_" + System.currentTimeMillis();

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
            paper.setStatus("APPROVED");
            paper.setApprovedBy(admin);
            studentPaperRepository.save(paper);

            // Award points to the uploader
            Users uploader = paper.getUploadedBy();
            if (uploader != null) {
                uploader.setContributionPoints(uploader.getContributionPoints() + 50);
                userRepo.save(uploader);
            }

            return ResponseEntity.ok("Paper approved and 50 points awarded to " + uploader.getUsername());
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
            paper.setStatus("REJECTED");
            paper.setRejectionReason(reason);
            studentPaperRepository.save(paper);

            return ResponseEntity.ok("Paper rejected successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error rejecting paper.");
        }
    }
}
