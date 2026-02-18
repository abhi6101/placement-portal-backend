package com.abhi.authProject.controller;

import com.abhi.authProject.model.Paper;
import com.abhi.authProject.repo.PaperRepository;
import com.abhi.authProject.service.FileStorageService;
import com.abhi.authProject.service.PaperBulkUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.io.ByteArrayOutputStream;

@RestController
@RequestMapping("/api")
public class PaperController {

    private final PaperRepository paperRepository;
    private final FileStorageService fileStorageService;
    private final PaperBulkUploadService bulkUploadService;

    @Value("${pdf.storage.directory:/tmp/resumes}")
    private String uploadDir;

    @Autowired
    public PaperController(PaperRepository paperRepository, FileStorageService fileStorageService,
            PaperBulkUploadService bulkUploadService) {
        this.paperRepository = paperRepository;
        this.fileStorageService = fileStorageService;
        this.bulkUploadService = bulkUploadService;
    }

    /**
     * Handles GET requests to /api/papers.
     * Can simpler filter by params.
     */
    @GetMapping("/papers")
    public ResponseEntity<List<Paper>> getAllPapers(
            @RequestParam(required = false) Integer semester,
            @RequestParam(required = false) String branch) {

        if (semester != null && branch != null) {
            return ResponseEntity.ok(paperRepository.findBySemesterAndBranchOrderByYearDesc(semester, branch));
        } else if (semester != null) {
            return ResponseEntity.ok(paperRepository.findBySemesterOrderByYearDesc(semester));
        } else if (branch != null) {
            return ResponseEntity.ok(paperRepository.findByBranchOrderByYearDesc(branch));
        } else {
            return ResponseEntity.ok(paperRepository.findAllByOrderByUploadedAtDesc());
        }
    }

    @GetMapping("/papers/semester/{semester}")
    public ResponseEntity<List<Paper>> getPapersBySemester(@PathVariable int semester) {
        List<Paper> papers = paperRepository.findBySemesterOrderByYearDesc(semester);
        return ResponseEntity.ok(papers);
    }

    /**
     * Handles POST requests to /api/papers/upload (Admin only).
     * Uploads a file and creates a paper record.
     */
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('DEPT_ADMIN')")
    @PostMapping(value = "/papers/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadPaper(
            @RequestParam("title") String title,
            @RequestParam("subject") String subject,
            @RequestParam("year") int year,
            @RequestParam("semester") int semester,
            @RequestParam("branch") String branch,
            @RequestParam(value = "company", required = false) String company,
            @RequestParam(value = "category", required = false, defaultValue = "End-Sem") String category,
            @RequestParam(value = "university", required = false, defaultValue = "DAVV") String university,
            @RequestParam("file") MultipartFile file) {
        try {
            // UPLOAD TO GOOGLE DRIVE
            String downloadUrl = fileStorageService.uploadFileToDrive(file);

            Paper paper = new Paper(title, subject, year, semester, branch, company, category, university, downloadUrl);
            Paper savedPaper = paperRepository.save(paper);
            return ResponseEntity.ok(savedPaper);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Failed to upload file: " + e.getMessage());
        }
    }

    /**
     * Handles POST requests to /api/papers/upload-multiple.
     * Allows uploading multiple files for the same subject/branch/semester.
     */
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('DEPT_ADMIN')")
    @PostMapping(value = "/papers/upload-multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadMultiplePapers(
            @RequestParam("title") String title,
            @RequestParam("subject") String subject,
            @RequestParam("year") int year,
            @RequestParam("semester") int semester,
            @RequestParam("branch") String branch,
            @RequestParam(value = "company", required = false) String company,
            @RequestParam(value = "category", required = false, defaultValue = "End-Sem") String category,
            @RequestParam(value = "university", required = false, defaultValue = "DAVV") String university,
            @RequestParam("files") MultipartFile[] files) {
        java.util.List<Paper> savedPapers = new java.util.ArrayList<>();
        try {
            // Ensure Branch exists
            bulkUploadService.ensureBranchExists(branch);

            for (MultipartFile file : files) {
                // UPLOAD TO CLOUDINARY
                String downloadUrl = fileStorageService.savePaperToCloudinary(file);

                // Priority: Extract year from filename if year is 0
                int finalYear = (year > 0) ? year : bulkUploadService.extractYear(file.getOriginalFilename(), 2024);

                // Use the original filename to distinguish between multiple papers if title is
                // generic
                String paperTitle = title;
                if (files.length > 1) {
                    String originalName = file.getOriginalFilename();
                    if (originalName != null && originalName.contains(".")) {
                        originalName = originalName.substring(0, originalName.lastIndexOf("."));
                    }
                    paperTitle = title + " (" + originalName + ")";
                }

                Paper paper = new Paper(paperTitle, subject, finalYear, semester, branch, company, category, university,
                        downloadUrl);
                savedPapers.add(paperRepository.save(paper));
            }
            return ResponseEntity.ok(savedPapers);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Failed to upload files: " + e.getMessage());
        }
    }

    /**
     * Bulk upload via ZIP file.
     * Structure: Branch/Semester X/Subject/File.pdf
     */
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('DEPT_ADMIN')")
    @PostMapping(value = "/papers/bulk-upload-zip", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> bulkUploadPapers(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "university", defaultValue = "DAVV") String university,
            @RequestParam(value = "year", defaultValue = "2024") int year) {
        try {
            if (!file.getOriginalFilename().toLowerCase().endsWith(".zip")) {
                return ResponseEntity.badRequest().body("Please upload a ZIP file.");
            }
            java.util.List<Paper> papers = bulkUploadService.processZipFile(file, university, year);
            return ResponseEntity.ok(papers);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Bulk upload failed: " + e.getMessage());
        }
    }

    /**
     * Legacy endpoint for JSON only if needed, but we prefer the upload one.
     */
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('DEPT_ADMIN')")
    @PostMapping("/papers")
    public ResponseEntity<Paper> addPaper(@RequestBody Paper paper) {
        Paper savedPaper = paperRepository.save(paper);
        return ResponseEntity.ok(savedPaper);
    }

    /**
     * Endpoint to serve the PDF file.
     */
    @GetMapping("/papers/batch-download")
    public ResponseEntity<byte[]> downloadBatch(
            @RequestParam String branch,
            @RequestParam int semester,
            @RequestParam(required = false) String subject) {

        List<Paper> papers;
        if (subject != null && !subject.isEmpty()) {
            papers = paperRepository.findBySemesterAndBranchAndSubjectOrderByYearDesc(semester, branch, subject);
        } else {
            papers = paperRepository.findBySemesterAndBranchOrderByYearDesc(semester, branch);
        }

        if (papers.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ZipOutputStream zos = new ZipOutputStream(baos)) {

            for (Paper paper : papers) {
                // Correctly resolve file path
                String fileName = paper.getPdfUrl().replace("/api/papers/download/", "");
                Path filePath = Paths.get(uploadDir, "papers").resolve(fileName).normalize();

                if (Files.exists(filePath)) {
                    ZipEntry entry = new ZipEntry(
                            paper.getSubject() + "/" + paper.getTitle() + "_" + paper.getYear() + ".pdf");
                    zos.putNextEntry(entry);
                    Files.copy(filePath, zos);
                    zos.closeEntry();
                }
            }
            zos.finish();

            String zipName = branch + "_Sem" + semester + (subject != null ? "_" + subject : "") + "_Papers.zip";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + zipName + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(baos.toByteArray());

        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/papers/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadPaper(@PathVariable String fileName) {
        try {
            // DEBUGGING LOGS
            System.out.println("Processing Download Request for: " + fileName);
            System.out.println("Configured uploadDir: " + uploadDir);

            Path baseDir = Paths.get(uploadDir, "papers");
            System.out.println("Base Directory: " + baseDir.toAbsolutePath());

            Path filePath = baseDir.resolve(fileName).normalize();
            System.out.println("Attempting to read file at: " + filePath.toAbsolutePath());

            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                System.out.println("File FOUND! Serving content.");
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            }

            // FALLBACK: Try resolving by replacing spaces with underscores
            // This handles cases where DB has "File Name.pdf" but Disk has "File_Name.pdf"
            if (fileName.contains(" ") || fileName.contains("%20")) {
                String sanitizedName = fileName.replace(" ", "_").replace("%20", "_");
                System.out.println("File not found. Trying sanitized fallback: " + sanitizedName);
                Path fallbackPath = baseDir.resolve(sanitizedName).normalize();
                Resource fallbackResource = new UrlResource(fallbackPath.toUri());

                if (fallbackResource.exists()) {
                    System.out.println("Fallback File FOUND!");
                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_PDF)
                            .header(HttpHeaders.CONTENT_DISPOSITION,
                                    "inline; filename=\"" + fallbackResource.getFilename() + "\"")
                            .body(fallbackResource);
                }
            }

            System.out.println("File NOT FOUND at path: " + filePath.toAbsolutePath());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            System.out.println("Exception during download: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('DEPT_ADMIN')")
    @DeleteMapping("/papers/{id}")
    public ResponseEntity<?> deletePaper(@PathVariable Long id) {
        if (!paperRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        paperRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/papers/proxy/{id}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> proxyDownload(@PathVariable Long id) {
        try {
            Paper paper = paperRepository.findById(id).orElseThrow(() -> new RuntimeException("Paper not found"));
            String fileUrl = paper.getPdfUrl();

            System.out.println("Redirecting download for ID: " + id + ", URL: " + fileUrl);

            if (fileUrl == null || !fileUrl.startsWith("http")) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.status(org.springframework.http.HttpStatus.FOUND)
                    .location(java.net.URI.create(fileUrl))
                    .build();
        } catch (Exception e) {
            System.out.println("Error in proxy download transform: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}