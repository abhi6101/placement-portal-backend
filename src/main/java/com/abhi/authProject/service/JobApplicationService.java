package com.abhi.authProject.service;

import com.abhi.authProject.model.JobApplicationRequest1;
import com.abhi.authProject.model.JobApplication;
import com.abhi.authProject.model.ApplicationStatus;
import com.abhi.authProject.repo.JobApplicationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.FileSystemResource;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class JobApplicationService {

    private final JavaMailSender mailSender;
    private final JobApplicationRepository jobApplicationRepository;

    @Value("${placement.portal.application.recipient-email}")
    private String recipientEmail;

    @Value("${SENDER_FROM_EMAIL}")
    private String senderEmail;

    @Value("${pdf.storage.directory:/tmp/resumes}")
    private String resumeStorageDirectory;

    public JobApplicationService(JavaMailSender mailSender, JobApplicationRepository jobApplicationRepository) {
        this.mailSender = mailSender;
        this.jobApplicationRepository = jobApplicationRepository;
    }

    // REMOVE THE @PostConstruct METHOD FROM HERE
    // public void init() throws IOException {
    //     Path path = Paths.get(pdfStorageDirectory);
    //     if (!Files.exists(path)) {
    //         Files.createDirectories(path);
    //     }
    // }

    @Transactional
    public JobApplication processJobApplication(JobApplicationRequest1 applicationRequest) throws MessagingException, IOException {
        String resumeFileName = generateUniqueResumeFileName(applicationRequest.getResume(), applicationRequest.getApplicantName());
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
                resumeFilePath.toString()
        );
        jobApplication = jobApplicationRepository.save(jobApplication);

        sendApplicantConfirmationEmail(jobApplication);
        sendHROrAdminNotificationEmail(jobApplication);

        System.out.println("Job application processed and saved: " + jobApplication.toString());
        return jobApplication;
    }

    @Transactional
    public JobApplication updateApplicationStatus(Long applicationId, ApplicationStatus newStatus) throws MessagingException, IOException {
        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Job Application not found with ID: " + applicationId));

        application.setStatus(newStatus);
        JobApplication updatedApplication = jobApplicationRepository.save(application);

        if (newStatus == ApplicationStatus.ACCEPTED) {
            sendAcceptedApplicationEmailToApplicant(updatedApplication);
        } else if (newStatus == ApplicationStatus.REJECTED) {
            sendRejectedApplicationEmailToApplicant(updatedApplication);
        }

        System.out.println("Application ID " + applicationId + " status updated to: " + newStatus);
        return updatedApplication;
    }

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

    private void sendApplicantConfirmationEmail(JobApplication application) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setFrom(senderEmail);
        helper.setTo(application.getApplicantEmail());
        helper.setSubject("Application Received: " + application.getJobTitle() + " at " + application.getCompanyName());

        String emailBody = "Dear " + application.getApplicantName() + ",<br><br>" +
                "Thank you for applying for the <strong>" + application.getJobTitle() + "</strong> position at <strong>" + application.getCompanyName() + "</strong>.<br><br>" +
                "Your application has been received and is currently under review. We will notify you of the next steps.<br><br>" +
                "Application ID: " + application.getId() + "<br>" +
                "Applied On: " + application.getAppliedAt().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")) + "<br><br>" +
                "Best regards,<br>The Placement Team";
        helper.setText(emailBody, true);

        File resumeFile = new File(application.getResumePath());
        if (resumeFile.exists()) {
            FileSystemResource file = new FileSystemResource(resumeFile);
            helper.addAttachment(resumeFile.getName(), file);
        }

        mailSender.send(message);
        System.out.println("Applicant confirmation email sent to: " + application.getApplicantEmail());
    }

    private void sendHROrAdminNotificationEmail(JobApplication application) throws MessagingException, IOException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setFrom(senderEmail);
        helper.setTo(recipientEmail);
        helper.setSubject("New Job Application: " + application.getJobTitle() + " from " + application.getApplicantName());

        StringBuilder emailBody = new StringBuilder();
        emailBody.append("<h3>New Job Application Received</h3>");
        emailBody.append("<p><strong>Application ID:</strong> ").append(application.getId()).append("</p>");
        emailBody.append("<p><strong>Job Title:</strong> ").append(application.getJobTitle()).append("</p>");
        emailBody.append("<p><strong>Company:</strong> ").append(application.getCompanyName()).append("</p>");
        emailBody.append("<p><strong>Applicant Name:</strong> ").append(application.getApplicantName()).append("</p>");
        emailBody.append("<p><strong>Applicant Email:</strong> ").append(application.getApplicantEmail()).append("</p>");
        emailBody.append("<p><strong>Applicant Phone:</strong> ").append(application.getApplicantPhone()).append("</p>");
        if (application.getApplicantRollNo() != null && !application.getApplicantRollNo().trim().isEmpty()) {
            emailBody.append("<p><strong>Applicant Roll No:</strong> ").append(application.getApplicantRollNo()).append("</p>");
        }
        if (application.getCoverLetter() != null && !application.getCoverLetter().trim().isEmpty()) {
            emailBody.append("<p><strong>Cover Letter:</strong></p>");
            emailBody.append("<p style='white-space: pre-wrap; border: 1px solid #eee; padding: 10px; background-color: #f9f9f9;'>").append(application.getCoverLetter()).append("</p>");
        }
        emailBody.append("<p>Application submitted on: ").append(application.getAppliedAt().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"))).append("</p>");

        helper.setText(emailBody.toString(), true);

        File resumeFile = new File(application.getResumePath());
        if (resumeFile.exists()) {
            FileSystemResource file = new FileSystemResource(resumeFile);
            helper.addAttachment(resumeFile.getName(), file);
        }

        mailSender.send(message);
        System.out.println("HR/Admin notification email sent.");
    }

    private void sendAcceptedApplicationEmailToApplicant(JobApplication application) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setFrom(senderEmail);
        helper.setTo(application.getApplicantEmail());
        helper.setSubject("Good News! Your Application for " + application.getJobTitle() + " has been Accepted!");

        String emailBody = "Dear " + application.getApplicantName() + ",<br><br>" +
                "We are pleased to inform you that your application for the <strong>" + application.getJobTitle() + "</strong> position at <strong>" + application.getCompanyName() + "</strong> has been accepted!<br><br>" +
                "The HR team will be in touch shortly to discuss the next steps and schedule your interview.<br><br>" +
                "Congratulations!<br><br>" +
                "Best regards,<br>The Placement Team";
        helper.setText(emailBody, true);
        mailSender.send(message);
        System.out.println("Accepted application email sent to: " + application.getApplicantEmail());
    }

    private void sendRejectedApplicationEmailToApplicant(JobApplication application) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setFrom(senderEmail);
        helper.setTo(application.getApplicantEmail());
        helper.setSubject("Update on your application for " + application.getJobTitle());

        String emailBody = "Dear " + application.getApplicantName() + ",<br><br>" +
                "Thank you for your interest in the <strong>" + application.getJobTitle() + "</strong> position at <strong>" + application.getCompanyName() + "</strong>.<br><br>" +
                "After careful consideration, we regret to inform you that we will not be moving forward with your application at this time.<br><br>" +
                "We wish you the best in your job search.<br><br>" +
                "Sincerely,<br>The Placement Team";
        helper.setText(emailBody, true);
        mailSender.send(message);
        System.out.println("Rejected application email sent to: " + application.getApplicantEmail());
    }
}