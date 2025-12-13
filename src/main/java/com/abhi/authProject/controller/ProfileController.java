package com.abhi.authProject.controller;

import com.abhi.authProject.model.Users;
import com.abhi.authProject.repo.UserRepo;
import com.abhi.authProject.service.ProfilePictureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    @Autowired
    private ProfilePictureService profilePictureService;

    @Autowired
    private UserRepo userRepo;

    @PostMapping("/picture")
    public ResponseEntity<?> uploadProfilePicture(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            Users user = userRepo.findByUsername(username);

            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "User not found"));
            }

            String imageUrl = profilePictureService.uploadProfilePicture(file, user);

            return ResponseEntity.ok(Map.of(
                    "message", "Profile picture uploaded successfully",
                    "url", imageUrl));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/picture/{userId}")
    public ResponseEntity<?> getProfilePicture(@PathVariable int userId) {
        Users user = userRepo.findById(userId).orElse(null);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(Map.of(
                "url", user.getProfilePictureUrl() != null ? user.getProfilePictureUrl() : ""));
    }

    @DeleteMapping("/picture")
    public ResponseEntity<?> deleteProfilePicture(Authentication authentication) {
        try {
            String username = authentication.getName();
            Users user = userRepo.findByUsername(username);

            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "User not found"));
            }

            profilePictureService.deleteProfilePicture(user);

            return ResponseEntity.ok(Map.of("message", "Profile picture deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
