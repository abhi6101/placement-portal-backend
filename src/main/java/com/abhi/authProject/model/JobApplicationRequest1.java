package com.abhi.authProject.model;

import org.springframework.web.multipart.MultipartFile;

public class JobApplicationRequest1 {

    
    private String jobId; // Corresponds to appliedJobId from frontend
    private String jobTitle;
    private String companyName;
    private String applicantName;
    private String applicantEmail;
    private String applicantPhone;
    private String applicantRollNo;
    private String coverLetter;
    private MultipartFile resume; // For the uploaded resume file

    // Constructors (you can add a no-arg constructor if needed)
    public JobApplicationRequest1() {
    }

    public JobApplicationRequest1(String jobId, String jobTitle, String companyName, String applicantName, String applicantEmail, String applicantPhone, String applicantRollNo, String coverLetter, MultipartFile resume) {
        this.jobId = jobId;
        this.jobTitle = jobTitle;
        this.companyName = companyName;
        this.applicantName = applicantName;
        this.applicantEmail = applicantEmail;
        this.applicantPhone = applicantPhone;
        this.applicantRollNo = applicantRollNo;
        this.coverLetter = coverLetter;
        this.resume = resume;
    }

    // Getters and Setters
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

    // UPDATE NEEDED: Correct the setter for applicantEmail
    public void setApplicantEmail(String applicantEmail) {
        this.applicantEmail = applicantEmail; // Corrected: Was 'applicant.getEmail()'
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

    public MultipartFile getResume() {
        return resume;
    }

    public void setResume(MultipartFile resume) {
        this.resume = resume;
    }

    @Override
    public String toString() {
        return "JobApplicationRequest1{" +
               "jobId='" + jobId + '\'' +
               ", jobTitle='" + jobTitle + '\'' +
               ", companyName='" + companyName + '\'' +
               ", applicantName='" + applicantName + '\'' +
               ", applicantEmail='" + applicantEmail + '\'' +
               ", applicantPhone='" + applicantPhone + '\'' +
               ", applicantRollNo='" + applicantRollNo + '\'' +
               ", coverLetter='" + (coverLetter != null ? "present" : "absent") + '\'' +
               ", resume=" + (resume != null ? resume.getOriginalFilename() : "null") +
               '}';
    }
}