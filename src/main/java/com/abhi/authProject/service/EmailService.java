package com.abhi.authProject.service;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Value("${brevo.api.key}")
    private String apiKey;

    @Value("${mail.from.email}")
    private String fromEmail;

    @Value("${mail.from.name}")
    private String fromName;

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    public void sendEmail(String toEmail, String subject, String htmlContent) throws IOException {
        sendEmailWithAttachment(toEmail, subject, htmlContent, null, null);
    }

    public void sendEmailWithAttachment(String toEmail, String subject, String htmlContent, byte[] attachment,
            String filename) throws IOException {
        try {
            JSONObject emailRequest = new JSONObject();

            // Sender
            JSONObject sender = new JSONObject();
            sender.put("name", fromName);
            sender.put("email", fromEmail);
            emailRequest.put("sender", sender);

            // Recipient
            JSONObject to = new JSONObject();
            to.put("email", toEmail);
            JSONArray toArray = new JSONArray();
            toArray.put(to);
            emailRequest.put("to", toArray);

            // Content
            emailRequest.put("subject", subject);
            emailRequest.put("htmlContent", htmlContent);

            // Attachment
            if (attachment != null && attachment.length > 0) {
                JSONObject attach = new JSONObject();
                attach.put("name", filename != null ? filename : "attachment.pdf");
                attach.put("content", Base64.getEncoder().encodeToString(attachment));
                JSONArray attachments = new JSONArray();
                attachments.put(attach);
                emailRequest.put("attachment", attachments);
            }

            // Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", apiKey);

            HttpEntity<String> entity = new HttpEntity<>(emailRequest.toString(), headers);

            ResponseEntity<String> response = restTemplate.postForEntity(BREVO_API_URL, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("✅ Email sent successfully via Brevo API to: {}", toEmail);
            } else {
                logger.error("❌ Failed to send email. Brevo returned: {}", response.getBody());
                throw new IOException("Brevo API error: " + response.getBody());
            }

        } catch (Exception e) {
            logger.error("❌ Error sending email via Brevo API: {}", e.getMessage());
            throw new IOException("Email sending failed", e);
        }
    }

    public void sendEmailWithAttachment(String toEmail, String subject, String htmlContent, byte[] attachment)
            throws IOException {
        sendEmailWithAttachment(toEmail, subject, htmlContent, attachment, "attachment.pdf");
    }

    public void sendEmailWithLocalFile(String toEmail, String subject, String htmlContent, String filePath)
            throws IOException {
        byte[] fileBytes = null;
        String filename = "attachment.pdf";
        if (filePath != null) {
            java.io.File file = new java.io.File(filePath);
            if (file.exists()) {
                fileBytes = Files.readAllBytes(Paths.get(filePath));
                filename = file.getName();
            }
        }
        sendEmailWithAttachment(toEmail, subject, htmlContent, fileBytes, filename);
    }

    // --- TEMPLATE METHODS ---

    public void sendVerificationEmail(String toEmail, String otp) throws IOException {
        String subject = "Placement Portal - Verify your email";
        String html = "<h3>Verify your email</h3><p>Your OTP is: <strong>" + otp + "</strong></p>";
        sendEmail(toEmail, subject, html);
    }

    public void sendPasswordResetEmail(String toEmail, String otp) throws IOException {
        String subject = "Password Reset Request";
        String html = "<h3>Reset your password</h3><p>Your OTP is: <strong>" + otp + "</strong></p>";
        sendEmail(toEmail, subject, html);
    }

    public void sendAccountCreatedEmail(String toEmail, String name, String tempPassword) throws IOException {
        String subject = "Welcome to the Placement Portal";
        String html = "<h3>Welcome, " + name + "!</h3><p>Your account has been created. Temporary password: <strong>"
                + tempPassword + "</strong></p>";
        sendEmail(toEmail, subject, html);
    }

    public void sendShortlistedEmail(String to, String name, String job, String company, String date, String time)
            throws IOException {
        String subject = "Shortlisted for " + company;
        String html = "<h3>Congratulations " + name + "!</h3>" +
                "<p>You have been shortlisted for the position of <strong>" + job + "</strong> at <strong>" + company
                + "</strong>.</p>" +
                "<p>Interview Details: " + date + " at " + time + "</p>";
        sendEmail(to, subject, html);
    }

    public void sendShortlistedEmail(String to, String name, String job, String company) throws IOException {
        sendShortlistedEmail(to, name, job, company, "TBD", "TBD");
    }

    public void sendAcceptanceEmail(String to, String name, String job, String company, String details)
            throws IOException {
        String subject = "Application Accepted - " + company;
        String html = "<h3>Congratulations " + name + "!</h3>" +
                "<p>Your application for <strong>" + job + "</strong> at <strong>" + company
                + "</strong> has been accepted.</p>" +
                "<p><strong>Details:</strong> " + details + "</p>";
        sendEmail(to, subject, html);
    }

    public void sendSelectedEmail(String to, String name, String job, String company) throws IOException {
        String subject = "Selected: Congratulations!";
        String html = "<h3>Great News " + name + "!</h3>" +
                "<p>You have been <strong>SELECTED</strong> by <strong>" + company + "</strong> for the <strong>" + job
                + "</strong> role.</p>";
        sendEmail(to, subject, html);
    }

    public void sendRejectedEmail(String to, String name, String job, String company) throws IOException {
        sendRejectionEmail(to, name, job, company);
    }

    public void sendRejectionEmail(String to, String name, String job, String company) throws IOException {
        String subject = "Application Status Update - " + company;
        String html = "<p>Dear " + name + ",</p>" +
                "<p>Thank you for your interest in the position of <strong>" + job + "</strong> at <strong>" + company
                + "</strong>.</p>" +
                "<p>We regret to inform you that we will not be moving forward with your application at this time.</p>";
        sendEmail(to, subject, html);
    }

    public void sendNewJobAlert(String to, String job, String company, String salary) throws IOException {
        String subject = "New Job Opportunity at " + company;
        String html = "<h3>New Job Posted</h3>" +
                "<p><strong>Company:</strong> " + company + "</p>" +
                "<p><strong>Position:</strong> " + job + "</p>" +
                "<p><strong>Salary:</strong> " + salary + "</p>";
        sendEmail(to, subject, html);
    }

    public void sendPasswordResetConfirmation(String toEmail) throws IOException {
        sendEmail(toEmail, "Password Changed Successfully", "<p>Your password has been changed as requested.</p>");
    }

    public void sendStatusUpdateEmail(String to, String name, String title, String company, String status)
            throws IOException {
        String subject = "Status Update: " + title;
        String html = "<h3>Application Status Update</h3>" +
                "<p>Hi " + name + ",</p>" +
                "<p>The status of your application for <strong>" + company + "</strong> (" + title
                + ") has been updated to: <strong>" + status + "</strong></p>";
        sendEmail(to, subject, html);
    }
}
