package com.abhi.authProject.controller;

import com.abhi.authProject.model.StudentProfile;
import com.abhi.authProject.model.Users;
import com.abhi.authProject.repo.StudentProfileRepo;
import com.abhi.authProject.repo.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin
@RequestMapping("/api/student-profile")
public class StudentProfileController {

    @Autowired
    private StudentProfileRepo profileRepo;

    @Autowired
    private UserRepo userRepo;

    @GetMapping
    public ResponseEntity<?> getMyProfile(Authentication auth) {
        String username = auth.getName();
        Users user = userRepo.findByUsername(username).orElse(null);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        return profileRepo.findByUserId(user.getId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createOrUpdateProfile(@RequestBody StudentProfile profile, Authentication auth) {
        String username = auth.getName();
        Users user = userRepo.findByUsername(username).orElse(null);

        if (user == null) {
            return ResponseEntity.badRequest().body("User not found");
        }

        // Sync with User entity for checkProfileStatus
        if (profile.getFullName() != null)
            user.setName(profile.getFullName());
        if (profile.getPhoneNumber() != null)
            user.setPhone(profile.getPhoneNumber());
        if (profile.getBranch() != null)
            user.setBranch(profile.getBranch());
        if (profile.getSemester() != null) {
            try {
                user.setSemester(Integer.parseInt(profile.getSemester()));
            } catch (NumberFormatException e) {
                // Ignore invalid format
            }
        }
        if (profile.getBatch() != null)
            user.setBatch(profile.getBatch());
        user.setLastProfileUpdate(java.time.LocalDate.now());
        userRepo.save(user);

        // Check if profile exists
        StudentProfile existing = profileRepo.findByUserId(user.getId()).orElse(null);

        if (existing != null) {
            // Update existing
            existing.setFullName(profile.getFullName());
            existing.setPhoneNumber(profile.getPhoneNumber());
            existing.setDateOfBirth(profile.getDateOfBirth());
            existing.setAddress(profile.getAddress());
            existing.setEnrollmentNumber(profile.getEnrollmentNumber());
            existing.setBranch(profile.getBranch());
            existing.setSemester(profile.getSemester());
            existing.setCgpa(profile.getCgpa());
            existing.setBacklogs(profile.getBacklogs());
            existing.setSkills(profile.getSkills());
            existing.setResumeUrl(profile.getResumeUrl());
            existing.setLinkedinProfile(profile.getLinkedinProfile());
            existing.setGithubProfile(profile.getGithubProfile());
            return ResponseEntity.ok(profileRepo.save(existing));
        } else {
            // Create new
            profile.setUser(user);
            return ResponseEntity.ok(profileRepo.save(profile));
        }
    }

    @GetMapping("/admin/all")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPT_ADMIN')")
    public ResponseEntity<?> getAllProfiles(Authentication auth) {
        String username = auth.getName();
        Users user = userRepo.findByUsername(username).orElse(null);

        if (user == null) {
            return ResponseEntity.status(401).body("User not found");
        }

        if (user.getRole().equals("SUPER_ADMIN") || user.getRole().equals("ADMIN")) {
            return ResponseEntity.ok(profileRepo.findAll());
        } else if (user.getRole().equals("DEPT_ADMIN")) {
            String branch = user.getBranch();
            if (branch == null || branch.isEmpty()) {
                return ResponseEntity.ok(java.util.Collections.emptyList());
            }
            return ResponseEntity.ok(profileRepo.findByBranch(branch));
        }

        return ResponseEntity.status(403).body("Access Denied");
    }

    @PutMapping("/{id}/status")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPT_ADMIN')")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestParam String status) {
        return profileRepo.findById(id).map(profile -> {
            profile.setApprovalStatus(status);
            return ResponseEntity.ok(profileRepo.save(profile));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/upload-id-card")
    public ResponseEntity<?> uploadIdCard(@RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            Authentication auth) throws java.io.IOException {
        String username = auth.getName();
        Users user = userRepo.findByUsername(username).orElse(null);
        if (user == null)
            return ResponseEntity.badRequest().body("User not found");

        StudentProfile profile = profileRepo.findByUserId(user.getId()).orElse(new StudentProfile());
        if (profile.getUser() == null)
            profile.setUser(user);

        com.abhi.authProject.model.IdCardImage img = new com.abhi.authProject.model.IdCardImage();
        img.setName(file.getOriginalFilename());
        img.setType(file.getContentType());
        img.setData(file.getBytes());

        profile.setIdCardImageEntity(img);
        profile.setIdCardUrl("/api/student-profile/id-card/" + profile.getId()); // Helper URL
        profileRepo.save(profile);
        return ResponseEntity.ok("ID Card Uploaded");
    }

    @GetMapping("/id-card/{id}")
    public ResponseEntity<org.springframework.core.io.Resource> getIdCard(@PathVariable Long id) {
        StudentProfile profile = profileRepo.findById(id).orElse(null);
        if (profile == null || profile.getIdCardImageEntity() == null)
            return ResponseEntity.notFound().build();

        com.abhi.authProject.model.IdCardImage img = profile.getIdCardImageEntity();

        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(img.getType()))
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + img.getName() + "\"")
                .body(new org.springframework.core.io.ByteArrayResource(img.getData()));
    }

    @PostMapping("/upload-aadhar")
    public ResponseEntity<?> uploadAadhar(@RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            Authentication auth) throws java.io.IOException {
        String username = auth.getName();
        Users user = userRepo.findByUsername(username).orElse(null);
        if (user == null)
            return ResponseEntity.badRequest().body("User not found");

        StudentProfile profile = profileRepo.findByUserId(user.getId()).orElse(new StudentProfile());
        if (profile.getUser() == null)
            profile.setUser(user);

        com.abhi.authProject.model.IdCardImage img = new com.abhi.authProject.model.IdCardImage();
        img.setName(file.getOriginalFilename());
        img.setType(file.getContentType());
        img.setData(file.getBytes());

        profile.setAadharImageEntity(img);
        profile.setAadharCardUrl("/api/student-profile/aadhar/" + profile.getId());
        profileRepo.save(profile);
        return ResponseEntity.ok("Aadhar Uploaded");
    }

    @GetMapping("/aadhar/{id}")
    public ResponseEntity<org.springframework.core.io.Resource> getAadhar(@PathVariable Long id) {
        StudentProfile profile = profileRepo.findById(id).orElse(null);
        if (profile == null || profile.getAadharImageEntity() == null)
            return ResponseEntity.notFound().build();
        com.abhi.authProject.model.IdCardImage img = profile.getAadharImageEntity();
        return ResponseEntity.ok().contentType(org.springframework.http.MediaType.parseMediaType(img.getType()))
                .body(new org.springframework.core.io.ByteArrayResource(img.getData()));
    }

    @PostMapping("/upload-admit-card")
    public ResponseEntity<?> uploadAdmitCard(@RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            Authentication auth) throws java.io.IOException {
        String username = auth.getName();
        Users user = userRepo.findByUsername(username).orElse(null);
        if (user == null)
            return ResponseEntity.badRequest().body("User not found");

        StudentProfile profile = profileRepo.findByUserId(user.getId()).orElse(new StudentProfile());
        if (profile.getUser() == null)
            profile.setUser(user);

        com.abhi.authProject.model.IdCardImage img = new com.abhi.authProject.model.IdCardImage();
        img.setName(file.getOriginalFilename());
        img.setType(file.getContentType());
        img.setData(file.getBytes());

        profile.setAdmitCardImageEntity(img);
        profile.setAdmitCardUrl("/api/student-profile/admit-card/" + profile.getId());
        profileRepo.save(profile);
        return ResponseEntity.ok("Admit Card Uploaded");
    }

    @GetMapping("/admit-card/{id}")
    public ResponseEntity<org.springframework.core.io.Resource> getAdmitCard(@PathVariable Long id) {
        StudentProfile profile = profileRepo.findById(id).orElse(null);
        if (profile == null || profile.getAdmitCardImageEntity() == null)
            return ResponseEntity.notFound().build();
        com.abhi.authProject.model.IdCardImage img = profile.getAdmitCardImageEntity();
        return ResponseEntity.ok().contentType(org.springframework.http.MediaType.parseMediaType(img.getType()))
                .body(new org.springframework.core.io.ByteArrayResource(img.getData()));
    }
}
