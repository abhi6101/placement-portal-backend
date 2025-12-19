package com.abhi.authProject.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "job_applications")
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String jobId; // ID of the job listing
    @Column(nullable = false)
    private String jobTitle;
    @Column(nullable = false)
    private String companyName;

    @Column(nullable = false)
    private String applicantName;
    @Column(nullable = false)
    private String applicantEmail;
    @Column(nullable = false)
    private String applicantPhone;
    private String applicantRollNo; // Optional

    @Column(columnDefinition = "TEXT") // For larger text fields
    private String coverLetter;

    @Column(nullable = false)
    private String resumePath; // Path to the stored resume file on the server

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status; // PENDING, ACCEPTED, REJECTED, INTERVIEW_SCHEDULED

    @Column(nullable = false, updatable = false)
    private LocalDateTime appliedAt;

    // You might want to link this to a User entity if you have one
    // @ManyToOne
    // @JoinColumn(name = "user_id")
    // private User applicantUser;

    // Constructor, Getters, Setters (Lombok can simplify this, but manually for
    // clarity)

    public JobApplication() {
        this.appliedAt = LocalDateTime.now();
        this.status = ApplicationStatus.PENDING; // Default status
    }

    // You can add a constructor for easy creation from JobApplicationRequest1
    public JobApplication(String jobId, String jobTitle, String companyName, String applicantName,
            String applicantEmail, String applicantPhone, String applicantRollNo,
            String coverLetter, String resumePath) {
        this(); // Call default constructor to set appliedAt and status
        this.jobId = jobId;
        this.jobTitle = jobTitle;
        this.companyName = companyName;
        this.applicantName = applicantName;
        this.applicantEmail = applicantEmail;
        this.applicantPhone = applicantPhone;
        this.applicantRollNo = applicantRollNo;
        this.coverLetter = coverLetter;
        this.resumePath = resumePath;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public void setApplicantName(String applicantName) {
        this.applicantName = applicantName;
    }

    public String getApplicantEmail() {
        return applicantEmail;
    }

    public void setApplicantEmail(String applicantEmail) {
        this.applicantEmail = applicantEmail;
    }

    public String getApplicantPhone() {
        return applicantPhone;
    }

    public void setApplicantPhone(String applicantPhone) {
        this.applicantPhone = applicantPhone;
    }

    public String getApplicantRollNo() {
        return applicantRollNo;
    }

    public void setApplicantRollNo(String applicantRollNo) {
        this.applicantRollNo = applicantRollNo;
    }

    public String getCoverLetter() {
        return coverLetter;
    }

    public void setCoverLetter(String coverLetter) {
        this.coverLetter = coverLetter;
    }

    public String getResumePath() {
        return resumePath;
    }

    public void setResumePath(String resumePath) {
        this.resumePath = resumePath;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public LocalDateTime getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(LocalDateTime appliedAt) {
        this.appliedAt = appliedAt;
    }

    @Override
    public String toString() {
        return "JobApplication{" +
                "id=" + id +
                ", jobId='" + jobId + '\'' +
                ", jobTitle='" + jobTitle + '\'' +
                ", companyName='" + companyName + '\'' +
                ", applicantName='" + applicantName + '\'' +
                ", status=" + status +
                '}';
    }
}