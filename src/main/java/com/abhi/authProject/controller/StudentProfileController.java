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
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> getAllProfiles() {
        return ResponseEntity.ok(profileRepo.findAll());
    }
}
