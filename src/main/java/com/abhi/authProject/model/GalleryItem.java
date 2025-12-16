package com.abhi.authProject.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "gallery_items")
public class GalleryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false) // IMAGE, VIDEO
    private String type;

    @Column(nullable = false, length = 1000)
    private String url; // Image URL or Video Link

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status; // PENDING, ACCEPTED, REJECTED (Reusing ApplicationStatus or create new?)
    // ApplicationStatus: PENDING, SHORTLISTED, SELECTED, REJECTED, ACCEPTED
    // It works.

    @Column(nullable = false)
    private String uploadedBy; // Username

    private LocalDateTime uploadedAt;

    public GalleryItem() {
        this.uploadedAt = LocalDateTime.now();
        this.status = ApplicationStatus.PENDING;
    }

    public GalleryItem(String title, String type, String url, String description, String uploadedBy) {
        this();
        this.title = title;
        this.type = type;
        this.url = url;
        this.description = description;
        this.uploadedBy = uploadedBy;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}
