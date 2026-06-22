package com.abhi.authProject.controller;

import com.abhi.authProject.model.Paper;
import com.abhi.authProject.repo.PaperRepository;
import com.abhi.authProject.service.FileStorageService;
import com.abhi.authProject.service.GlobalSettingsService;
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

    @Autowired
    private GlobalSettingsService settingsService;

    @GetMapping("/papers/settings")
    public ResponseEntity<?> getPaperSettings() {
        boolean downloadEnabled = settingsService.getSettings().isPaperDownloadEnabled();
        boolean screenshotRestrictionEnabled = settingsService.getSettings().isScreenshotRestrictionEnabled();
        boolean paperWithoutLoginEnabled = settingsService.getSettings().isPaperWithoutLoginEnabled();
        boolean notesWithoutLoginEnabled = settingsService.getSettings().isNotesWithoutLoginEnabled();
        boolean notesDownloadEnabled = settingsService.getSettings().isNotesDownloadEnabled();
        java.util.Map<String, Boolean> response = new java.util.HashMap<>();
        response.put("paperDownloadEnabled", downloadEnabled);
        response.put("notesDownloadEnabled", notesDownloadEnabled);
        response.put("screenshotRestrictionEnabled", screenshotRestrictionEnabled);
        response.put("paperWithoutLoginEnabled", paperWithoutLoginEnabled);
        response.put("notesWithoutLoginEnabled", notesWithoutLoginEnabled);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/papers/download/{id}")
    public ResponseEntity<?> downloadPaper(
            @PathVariable Long id,
            @org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "VIEW") String action) {
        
        // STRICT SECURITY: Only authenticated users can download or view PDFs, unless paperWithoutLoginEnabled is enabled
        boolean paperWithoutLogin = settingsService.getSettings().isPaperWithoutLoginEnabled();
        org.springframework.security.core.Authentication currentAuth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = currentAuth != null && currentAuth.isAuthenticated() && !"anonymousUser".equals(currentAuth.getName());
        
        if (!paperWithoutLogin && !isAuthenticated) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .body("Please Login or Register to access this paper.");
        }

        try {
            // Check if suspended
            try {
                org.springframework.security.core.Authentication auth = currentAuth;
                if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                    String username = auth.getName();
                    com.abhi.authProject.model.Users user = userRepo.findByComputerCodeOrUsername(username).orElse(null);
                    if (user != null && user.getLockedUntil() != null) {
                        if (user.getLockedUntil().isAfter(java.time.LocalDateTime.now())) {
                            long secondsLeft = java.time.Duration.between(java.time.LocalDateTime.now(), user.getLockedUntil()).getSeconds();
                            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                                    .body("Your account is temporarily suspended due to security violations. Please try again in " + secondsLeft + " seconds.");
                        } else {
                            // Automatically clear strikes once locked period expires
                            user.setSecurityStrikes(0);
                            user.setLockedUntil(null);
                            userRepo.save(user);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Suspension check failed: " + e.getMessage());
            }

            Paper paper = paperRepository.findById(id).orElseThrow(() -> new RuntimeException("Paper not found"));
            
            // Retrieve current authenticated user and log the view/download
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
                            paper.getYear(),
                            action
                        );
                        paperViewLogRepository.save(viewLog);
                    }
                } else {
                    // Log guest access
                    com.abhi.authProject.model.PaperViewLog viewLog = new com.abhi.authProject.model.PaperViewLog(
                        "anonymousUser",
                        "Guest",
                        "GUEST",
                        paper.getId(),
                        paper.getTitle(),
                        paper.getSubject(),
                        paper.getBranch(),
                        paper.getSemester(),
                        paper.getYear(),
                        action
                    );
                    paperViewLogRepository.save(viewLog);
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

    @org.springframework.web.bind.annotation.PostMapping("/papers/violation")
    public ResponseEntity<?> logSecurityViolation() {
        try {
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body("User not authenticated");
            }
            
            String username = auth.getName();
            com.abhi.authProject.model.Users user = userRepo.findByComputerCodeOrUsername(username).orElse(null);
            if (user == null) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).body("User not found");
            }

            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            if (user.getLastStrikeTime() != null && now.isAfter(user.getLastStrikeTime().plusMinutes(10))) {
                user.setSecurityStrikes(0);
            }

            int currentStrikes = user.getSecurityStrikes() + 1;
            user.setSecurityStrikes(currentStrikes);
            user.setLastStrikeTime(now);

            boolean isLocked = false;
            long secondsLeft = 0;

            if (currentStrikes >= 5) {
                // If their last lockout was on a previous calendar day, reset count to 0 first
                if (user.getLockedUntil() != null && user.getLockedUntil().toLocalDate().isBefore(java.time.LocalDate.now())) {
                    user.setLockoutCount(0);
                }

                int lockoutCount = user.getLockoutCount() + 1;
                user.setLockoutCount(lockoutCount);

                int durationMinutes;
                switch (lockoutCount) {
                    case 1:  durationMinutes = 2; break;
                    case 2:  durationMinutes = 5; break;
                    case 3:  durationMinutes = 10; break;
                    case 4:  durationMinutes = 20; break;
                    case 5:  durationMinutes = 30; break;
                    case 6:  durationMinutes = 60; break;
                    case 7:  durationMinutes = 120; break;
                    case 8:  durationMinutes = 240; break;
                    case 9:  durationMinutes = 480; break;
                    case 10: durationMinutes = 600; break;
                    default: durationMinutes = 1440; break; // 24 hours
                }

                java.time.LocalDateTime lockedUntil = java.time.LocalDateTime.now().plusMinutes(durationMinutes);
                user.setLockedUntil(lockedUntil);
                user.setSecurityStrikes(0); // Clean slate for strikes
                isLocked = true;
                secondsLeft = durationMinutes * 60L;
            }

            userRepo.save(user);

            int finalLockoutCount = user.getLockoutCount();
            long finalSecondsLeft = secondsLeft;
            boolean finalIsLocked = isLocked;

            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("strikes", finalIsLocked ? 0 : currentStrikes);
            response.put("isLocked", finalIsLocked);
            response.put("secondsLeft", finalSecondsLeft);
            
            String lockoutMsg = "Your account has been locked for " + 
                (finalSecondsLeft >= 3600 ? (finalSecondsLeft / 3600 + " hour(s)") : (finalSecondsLeft / 60 + " minute(s)")) + 
                " due to repeated security violations.";
            response.put("message", finalIsLocked ? lockoutMsg : "Security violation registered.");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}
