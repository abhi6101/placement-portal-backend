package com.abhi.authProject.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "resume_files")
@Data
@NoArgsConstructor
public class ResumeFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    private String fileName;

    private String contentType;

    @Lob
    @Column(name = "file_data", nullable = false)
    private byte[] fileData;

    public ResumeFile(Users user, String fileName, String contentType, byte[] fileData) {
        this.user = user;
        this.fileName = fileName;
        this.contentType = contentType;
        this.fileData = fileData;
    }
}
