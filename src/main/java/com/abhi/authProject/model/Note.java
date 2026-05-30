package com.abhi.authProject.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Date;

@Entity
@Table(name = "notes")
public class Note {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String subject;
    private Integer semester; // can be null/0 for all semesters
    private String branch;    // can be null/empty for all branches
    private String visibility; // ALL, BRANCH, ADMIN
    private String pdfUrl;
    private String relativePath; // preserved relative directory path, e.g. "Soft Computing/Neural Networks/Notes-1.pdf"
    private String rootFolder;   // the parent uploaded folder, e.g. "Soft Computing"
    private Date uploadedAt;

    public Note() {
        this.uploadedAt = new Date();
    }

    public Note(String title, String subject, Integer semester, String branch, String visibility, String pdfUrl, String relativePath, String rootFolder) {
        this.title = title;
        this.subject = subject != null ? subject.toLowerCase() : "";
        this.semester = semester;
        this.branch = branch;
        this.visibility = visibility != null ? visibility.toUpperCase() : "ALL";
        this.pdfUrl = pdfUrl;
        this.relativePath = relativePath;
        this.rootFolder = rootFolder;
        this.uploadedAt = new Date();
    }

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

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public Integer getSemester() {
        return semester;
    }

    public void setSemester(Integer semester) {
        this.semester = semester;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public String getPdfUrl() {
        return pdfUrl;
    }

    public void setPdfUrl(String pdfUrl) {
        this.pdfUrl = pdfUrl;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }

    public String getRootFolder() {
        return rootFolder;
    }

    public void setRootFolder(String rootFolder) {
        this.rootFolder = rootFolder;
    }

    public Date getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(Date uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    @Override
    public String toString() {
        return "Note{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", subject='" + subject + '\'' +
                ", semester=" + semester +
                ", branch='" + branch + '\'' +
                ", visibility='" + visibility + '\'' +
                ", pdfUrl='" + pdfUrl + '\'' +
                ", relativePath='" + relativePath + '\'' +
                ", rootFolder='" + rootFolder + '\'' +
                ", uploadedAt=" + uploadedAt +
                '}';
    }
}
