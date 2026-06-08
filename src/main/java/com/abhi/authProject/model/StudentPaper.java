package com.abhi.authProject.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "student_papers")
public class StudentPaper {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "subject", nullable = false)
    private String subject; // e.g. "Network Security" or "NS"

    @Column(name = "semester", nullable = false)
    private Integer semester;

    @Column(name = "branch", nullable = false)
    private String branch; // e.g. "MCA", "IMCA"

    @Column(name = "year", nullable = false)
    private String year; // e.g. "2025" or "JUN-2025"

    // Enum mapping could be used, but String is simpler for now
    @Column(name = "status", nullable = false)
    private String status = "PENDING"; // "PENDING", "APPROVED", "REJECTED"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "password"})
    private Users uploadedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "password"})
    private Users approvedBy;

    // We store the Google Drive file ID once it's compiled and uploaded
    @Column(name = "drive_file_id", length = 255)
    private String driveFileId;
    
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
