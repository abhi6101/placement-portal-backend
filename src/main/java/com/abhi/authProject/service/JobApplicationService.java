package com.abhi.authProject.service;

import com.abhi.authProject.model.ApplicationStatus;
import com.abhi.authProject.model.JobApplication;
import com.abhi.authProject.model.JobApplicationRequest1;
import com.abhi.authProject.repo.JobApplicationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class JobApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(JobApplicationService.class);

    // --- DEPENDENCIES ARE NOW UPDATED ---
    private final SendGridEmailService sendGridEmailService;
    private final EmailService emailService;
    private final JobApplicationRepository jobApplicationRepository;

    @Value("${placement.portal.application.recipient-email}")
    private String recipientEmail;

    @Value("${pdf.storage.directory:/tmp/resumes}")
    private String resumeStorageDirectory;

    @Autowired
    public JobApplicationService(SendGridEmailService sendGridEmailService,
            EmailService emailService,
            JobApplicationRepository jobApplicationRepository) {
        this.sendGridEmailService = sendGridEmailService;
        this.emailService = emailService;
        this.jobApplicationRepository = jobApplicationRepository;
    }

    @Transactional
    public JobApplication processJobApplication(JobApplicationRequest1 applicationRequest) throws IOException {
        String resumeFileName = generateUniqueResumeFileName(applicationRequest.getResume(),
                applicationRequest.getApplicantName());
        Path resumeFilePath = saveResumeLocally(applicationRequest.getResume(), resumeFileName);

        JobApplication jobApplication = new JobApplication(
                applicationRequest.getJobId(),
                applicationRequest.getJobTitle(),
                applicationRequest.getCompanyName(),
                applicationRequest.getApplicantName(),
                applicationRequest.getApplicantEmail(),
                applicationRequest.getApplicantPhone(),
                applicationRequest.getApplicantRollNo(),
                applicationRequest.getCoverLetter(),
                resumeFilePath.toString());
        jobApplication = jobApplicationRepository.save(jobApplication);

        // We wrap email sending in a try-catch so that if email fails, the application
        // is still saved.
        try {
            sendApplicantConfirmationEmail(jobApplication);
            sendHROrAdminNotificationEmail(jobApplication);
        } catch (Exception e) {
            logger.error("Application {} was saved successfully, but sending notification emails failed.",
                    jobApplication.getId(), e);
        }

        logger.info("Job application processed and saved: {}", jobApplication);
        return jobApplication;
    }

    @Transactional
    public JobApplication updateApplicationStatus(Long applicationId, ApplicationStatus newStatus) {
        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Job Application not found with ID: " + applicationId));

        application.setStatus(newStatus);
        JobApplication updatedApplication = jobApplicationRepository.save(application);

        try {
            // New Unified Email Notification
            emailService.sendStatusUpdateEmail(
                    updatedApplication.getApplicantEmail(),
                    updatedApplication.getApplicantName(),
                    updatedApplication.getJobTitle(),
                    updatedApplication.getCompanyName(),
                    newStatus.name() // Pass the status string (SHORTLISTED, SELECTED, REJECTED)
            );
            logger.info("Application status update email sent to: {}", updatedApplication.getApplicantEmail());

        } catch (Exception e) {
            logger.error("Status for application {} was updated, but the notification email failed to send.",
                    applicationId, e);
        }

        logger.info("Application ID {} status updated to: {}", applicationId, newStatus);
        return updatedApplication;
    }

    // --- NO CHANGES to helper methods below, only to email methods ---

    private void sendApplicantConfirmationEmail(JobApplication application) throws IOException {
        String subject = "Application Received: " + application.getJobTitle() + " at " + application.getCompanyName();
        String emailBody = "Dear " + application.getApplicantName() + ",<br><br>" +
                "Thank you for applying for the <strong>" + application.getJobTitle() + "</strong> position at <strong>"
                + application.getCompanyName() + "</strong>.<br><br>" +
                "Your application has been received and is currently under review. We will notify you of the next steps.<br><br>"
                +
                "Application ID: " + application.getId() + "<br>" +
                "Applied On: " + application.getAppliedAt().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"))
                + "<br><br>" +
                "Best regards,<br>The Placement Team";

        // Use the SendGrid service for application confirmation
        sendGridEmailService.sendEmailWithAttachment(application.getApplicantEmail(), subject, emailBody,
                application.getResumePath());
        logger.info("Applicant confirmation email sent to: {}", application.getApplicantEmail());
    }

    private void sendHROrAdminNotificationEmail(JobApplication application) throws IOException {
        String subject = "New Job Application: " + application.getJobTitle() + " from "
                + application.getApplicantName();
        StringBuilder emailBody = new StringBuilder()
                .append("<h3>New Job Application Received</h3>")
                .append("<p><strong>Application ID:</strong> ").append(application.getId()).append("</p>")
                .append("<p><strong>Job Title:</strong> ").append(application.getJobTitle()).append("</p>")
                .append("<p><strong>Company:</strong> ").append(application.getCompanyName()).append("</p>")
                .append("<p><strong>Applicant Name:</strong> ").append(application.getApplicantName()).append("</p>")
                .append("<p><strong>Applicant Email:</strong> ").append(application.getApplicantEmail()).append("</p>")
                .append("<p><strong>Applicant Phone:</strong> ").append(application.getApplicantPhone()).append("</p>");
        if (application.getApplicantRollNo() != null && !application.getApplicantRollNo().trim().isEmpty()) {
            emailBody.append("<p><strong>Applicant Roll No:</strong> ").append(application.getApplicantRollNo())
                    .append("</p>");
        }
        if (application.getCoverLetter() != null && !application.getCoverLetter().trim().isEmpty()) {
            emailBody.append(
                    "<p><strong>Cover Letter:</strong></p><p style='white-space: pre-wrap; border: 1px solid #eee; padding: 10px; background-color: #f9f9f9;'>")
                    .append(application.getCoverLetter()).append("</p>");
        }
        emailBody.append("<p>Application submitted on: ")
                .append(application.getAppliedAt().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")))
                .append("</p>");

        // Use the SendGrid service for HR notification
        sendGridEmailService.sendEmailWithAttachment(recipientEmail, subject, emailBody.toString(),
                application.getResumePath());
        logger.info("HR/Admin notification email sent to: {}", recipientEmail);
    }

    // --- NO CHANGES to the methods below ---

    public List<JobApplication> getAllJobApplications() {
        return jobApplicationRepository.findAll();
    }

    public List<JobApplication> getJobApplicationsByStatus(ApplicationStatus status) {
        return jobApplicationRepository.findByStatus(status);
    }

    public Optional<JobApplication> getJobApplicationById(Long id) {
        return jobApplicationRepository.findById(id);
    }

    private Path saveResumeLocally(MultipartFile resumeFile, String fileName) throws IOException {
        Path uploadPath = Paths.get(resumeStorageDirectory);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(resumeFile.getInputStream(), filePath);
        System.out.println("Resume saved to: " + filePath.toAbsolutePath());
        return filePath;
    }

    private String generateUniqueResumeFileName(MultipartFile resumeFile, String applicantName) {
        String originalFilename = resumeFile.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String sanitizedApplicantName = applicantName.replaceAll("[^a-zA-Z0-9.-]", "_");
        return sanitizedApplicantName + "_" + System.currentTimeMillis() + fileExtension;
    }

    private void sendAcceptedApplicationEmailToApplicant(JobApplication application) throws IOException {
        String subject = "Good News! Your Application for " + application.getJobTitle() + " has been Accepted!";
        String emailBody = "Dear " + application.getApplicantName() + ",<br><br>" +
                "We are pleased to inform you that your application for the <strong>" + application.getJobTitle()
                + "</strong> position at <strong>" + application.getCompanyName()
                + "</strong> has been accepted!<br><br>" +
                "The HR team will be in touch shortly to discuss the next steps and schedule your interview.<br><br>" +
                "Congratulations!<br><br>" +
                "Best regards,<br>The Placement Team";
        sendGridEmailService.sendEmailWithAttachment(application.getApplicantEmail(), subject, emailBody, null);
        logger.info("Accepted application email sent to: {}", application.getApplicantEmail());
    }

    private void sendRejectedApplicationEmailToApplicant(JobApplication application) throws IOException {
        String subject = "Update on your application for " + application.getJobTitle();
        String emailBody = "Dear " + application.getApplicantName() + ",<br><br>" +
                "Thank you for your interest in the <strong>" + application.getJobTitle()
                + "</strong> position at <strong>" + application.getCompanyName() + "</strong>.<br><br>" +
                "After careful consideration, we regret to inform you that we will not be moving forward with your application at this time.<br><br>"
                +
                "We wish you the best in your job search.<br><br>" +
                "Sincerely,<br>The Placement Team";
        sendGridEmailService.sendEmailWithAttachment(application.getApplicantEmail(), subject, emailBody, null);
        logger.info("Rejected application email sent to: {}", application.getApplicantEmail());
    }
}