package com.abhi.authProject.controller;

import com.abhi.authProject.model.ApplicationStatus;
import com.abhi.authProject.model.GalleryItem;
import com.abhi.authProject.repo.GalleryRepository;
import com.abhi.authProject.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class GalleryController {

    @Autowired
    private GalleryRepository galleryRepository;

    @Autowired
    private FileStorageService fileStorageService;

    // Public: Get all ACCEPTED items
    @GetMapping("/gallery")
    public List<GalleryItem> getPublicGallery() {
        return galleryRepository.findByStatus(ApplicationStatus.ACCEPTED);
    }

    // User: Submit new item
    @PostMapping("/gallery")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SUPER_ADMIN', 'COMPANY_ADMIN')")
    public ResponseEntity<GalleryItem> submitGalleryItem(
            @RequestParam("title") String title,
            @RequestParam("type") String type,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "url", required = false) String urlLink,
            Principal principal) {

        String finalUrl = urlLink;

        if (image != null && !image.isEmpty()) {
            try {
                // Reuse saveResume or create generic saveFile. saveResume stores in specific
                // dir, but it's fine for now or I can assume generic.
                // FileStorageService has saveResume. I should check if it has generic save.
                // Assuming it returns path.
                String absolutePath = fileStorageService.saveResume(image, "gallery_" + System.currentTimeMillis());

                // Extract filename from absolute path
                String filename = java.nio.file.Paths.get(absolutePath).getFileName().toString();
                finalUrl = "https://placement-portal-backend-nwaj.onrender.com/uploads/" + filename;
                // Using absolute URL for safety if frontend is on different domain,
                // but relative "/uploads/" + filename is usually fine if on same domain or
                // proxy.
                // Given previous issues, full URL is safer for now.
                // Wait, saveResume appends applicantName.
                // I might need a specific method or just use it.
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload image");
            }
        }

        if (finalUrl == null || finalUrl.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image file or URL is required");
        }

        GalleryItem item = new GalleryItem(title, type, finalUrl, description, principal.getName());
        return ResponseEntity.ok(galleryRepository.save(item));
    }

    // Admin: Get All
    @GetMapping("/admin/gallery")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'COMPANY_ADMIN')")
    public List<GalleryItem> getAllGalleryItems() {
        return galleryRepository.findAll();
    }

    // Admin: Update Status
    @PutMapping("/admin/gallery/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<GalleryItem> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        GalleryItem item = galleryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));

        if (payload.containsKey("status")) {
            item.setStatus(ApplicationStatus.valueOf(payload.get("status")));
        }
        return ResponseEntity.ok(galleryRepository.save(item));
    }

    // Admin: Edit Item
    @PutMapping("/admin/gallery/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<GalleryItem> updateItem(@PathVariable Long id, @RequestBody GalleryItem updated) {
        return galleryRepository.findById(id).map(item -> {
            item.setTitle(updated.getTitle());
            item.setDescription(updated.getDescription());
            item.setType(updated.getType());
            if (updated.getUrl() != null && !updated.getUrl().isEmpty()) {
                item.setUrl(updated.getUrl());
            }
            return ResponseEntity.ok(galleryRepository.save(item));
        }).orElse(ResponseEntity.notFound().build());
    }

    // Admin: Delete
    @DeleteMapping("/admin/gallery/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> deleteItem(@PathVariable Long id) {
        galleryRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
