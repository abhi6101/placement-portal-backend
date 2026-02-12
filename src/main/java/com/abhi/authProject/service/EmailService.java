package com.abhi.authProject.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Value("${mail.from.email}")
    private String fromEmail;

    @Value("${mail.from.name}")
    private String fromName;

    @Autowired
    private GlobalSettingsService globalSettingsService;

    @Autowired
    private JavaMailSender mailSender;

    public void sendEmail(String toEmail, String subject, String htmlContent) throws IOException {
        if (!globalSettingsService.isEmailAllowed()) {
            logger.info("Email sending is DISABLED (Master). Skipping: {}", toEmail);
            return;
        }

        try {
            logger.info("📧 Sending email via Gmail SMTP to: {}", toEmail);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromName + " <" + fromEmail + ">");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            logger.info("✅ Email sent successfully via Gmail SMTP to: {}", toEmail);
        } catch (MessagingException e) {
            logger.error("❌ Failed to send email via Gmail SMTP to {}: {}", toEmail, e.getMessage());
            throw new IOException("Email sending failed: " + e.getMessage());
        }
    }

    public void sendEmailWithAttachment(String toEmail, String subject, String htmlContent, byte[] attachment,
            String filename) throws IOException {
        if (!globalSettingsService.isEmailAllowed()) {
            logger.info("Email sending is DISABLED (Master). Skipping: {}", toEmail);
            return;
        }

        try {
            logger.info("📧 Sending email with attachment via Gmail SMTP to: {}", toEmail);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromName + " <" + fromEmail + ">");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            if (attachment != null && attachment.length > 0) {
                helper.addAttachment(filename != null ? filename : "attachment.pdf", new ByteArrayResource(attachment));
            }

            mailSender.send(message);
            logger.info("✅ Email with attachment sent successfully via Gmail SMTP to: {}", toEmail);
        } catch (MessagingException e) {
            logger.error("❌ Failed to send email with attachment to {}: {}", toEmail, e.getMessage());
            throw new IOException("Email sending with attachment failed: " + e.getMessage());
        }
    }

    // Overloaded to support old calls that didn't pass filename
    public void sendEmailWithAttachment(String toEmail, String subject, String htmlContent, byte[] attachment)
            throws IOException {
        sendEmailWithAttachment(toEmail, subject, htmlContent, attachment, null);
    }

    public void sendEmailWithLocalFile(String toEmail, String subject, String htmlContent, String filePath)
            throws IOException {
        try {
            byte[] fileContent = Files.readAllBytes(Paths.get(filePath));
            String fileName = Paths.get(filePath).getFileName().toString();
            sendEmailWithAttachment(toEmail, subject, htmlContent, fileContent, fileName);
        } catch (Exception e) {
            logger.warn("Could not attach file {}, sending email without it. Error: {}", filePath, e.getMessage());
            sendEmail(toEmail, subject, htmlContent);
        }
    }

    // --- Template Methods ---

    public void sendPasswordResetEmail(String toEmail, String otp) throws IOException {
        String subject = "Your Password Reset OTP";
        String htmlContent = "<div style='font-family: Arial, sans-serif; padding: 20px; border: 1px solid #ddd;'>" +
                "<h2>Password Reset Request</h2>" +
                "<p>Hello,</p>" +
                "<p>You requested a password reset. Use the following OTP to reset your password:</p>" +
                "<h1 style='color: #007bff;'>" + otp + "</h1>" +
                "<p>This OTP is valid for 10 minutes. If you did not request this, please ignore this email.</p>" +
                "<br><p>Best regards,<br>Hack2Hired Team</p></div>";
        sendEmail(toEmail, subject, htmlContent);
    }

    public void sendPasswordResetConfirmation(String toEmail) throws IOException {
        String subject = "Password Reset Successful";
        String htmlContent = "<h3>Hello,</h3>" +
                "<p>Your password has been successfully reset. If you did not perform this action, please contact support immediately.</p>"
                +
                "<br><p>Best regards,<br>Hack2Hired Team</p>";
        sendEmail(toEmail, subject, htmlContent);
    }

    public void sendAcceptanceEmail(String toEmail, String name, String jobTitle, String company,
            String interviewDetails) throws IOException {
        String subject = "Congratulations! You are Shortlisted for " + jobTitle;
        String htmlContent = "<h3>Dear " + name + ",</h3>" +
                "<p>We are pleased to inform you that you have been <b>shortlisted</b> for the <b>" + jobTitle
                + "</b> position at <b>" + company + "</b>.</p>" +
                "<p><b>Next Steps:</b> " + interviewDetails + "</p>" +
                "<p>Please check your portal for more details.</p>";
        sendEmail(toEmail, subject, htmlContent);
    }

    public void sendRejectionEmail(String toEmail, String name, String jobTitle, String company) throws IOException {
        String subject = "Status Update regarding your application for " + jobTitle;
        String htmlContent = "<h3>Dear " + name + ",</h3>" +
                "<p>Thank you for your interest in the <b>" + jobTitle + "</b> position at <b>" + company + "</b>.</p>"
                +
                "<p>After careful consideration, we regret to inform you that we will not be moving forward with your application at this time.</p>"
                +
                "<p>We wish you the best in your career pursuits.</p>";
        sendEmail(toEmail, subject, htmlContent);
    }

    public void sendStatusUpdateEmail(String toEmail, String name, String jobTitle, String company, String status)
            throws IOException {
        String subject = "Application Status Update: " + jobTitle;
        String htmlContent = "<h3>Dear " + name + ",</h3>" +
                "<p>The status of your application for <b>" + jobTitle + "</b> at <b>" + company
                + "</b> has been updated to: <b>" + status + "</b>.</p>" +
                "<p>Please log in to the placement portal for more details.</p>";
        sendEmail(toEmail, subject, htmlContent);
    }

    public void sendShortlistedEmail(String toEmail, String name, String jobTitle, String company) throws IOException {
        String subject = "Shortlisted for " + jobTitle;
        String htmlContent = "<h3>Congratulations " + name + "!</h3>" +
                "<p>You have been <b>shortlisted</b> for the <b>" + jobTitle + "</b> position at <b>" + company
                + "</b>.</p>" +
                "<p>Stay tuned for further updates regarding the interview process.</p>";
        sendEmail(toEmail, subject, htmlContent);
    }

    public void sendShortlistedEmail(String toEmail, String name, String jobTitle, String company, String date,
            String venue) throws IOException {
        String subject = "Interview Invitation: " + company;
        String htmlContent = "<h3>Congratulations " + name + "!</h3>" +
                "<p>You are shortlisted for an interview with <b>" + company + "</b> for the <b>" + jobTitle
                + "</b> position.</p>" +
                "<p><b>Date:</b> " + date + "</p>" +
                "<p><b>Venue/Link:</b> " + venue + "</p>" +
                "<p>Please log in to the portal for more details.</p>";
        sendEmail(toEmail, subject, htmlContent);
    }

    public void sendSelectedEmail(String toEmail, String name, String jobTitle, String company) throws IOException {
        String subject = "Selected for " + jobTitle + "!";
        String htmlContent = "<h3>Hurray " + name + "!</h3>" +
                "<p>We are delighted to inform you that you have been <b>selected</b> for the <b>" + jobTitle
                + "</b> position at <b>" + company + "</b>.</p>" +
                "<p>Congratulations on your success!</p>";
        sendEmail(toEmail, subject, htmlContent);
    }

    public void sendRejectedEmail(String toEmail, String name, String jobTitle, String company) throws IOException {
        sendRejectionEmail(toEmail, name, jobTitle, company);
    }

    public void sendAccountUpgradeConfirmation(String toEmail, String name, String newRole) throws IOException {
        String subject = "Account Upgraded - Placement Portal";
        String htmlContent = "<h3>Hello " + name + ",</h3>" +
                "<p>Your account has been upgraded. Your new role is: <b>" + newRole + "</b>.</p>" +
                "<p>You now have access to additional features in the portal.</p>";
        sendEmail(toEmail, subject, htmlContent);
    }

    public void sendAccountCreatedEmail(String toEmail, String name, String role, String tempPassword)
            throws IOException {
        String subject = "Account Created - Placement Portal";
        String htmlBody = "<h3>Welcome " + name + "!</h3>" +
                "<p>Your account has been created by the Admin.</p>" +
                "<p><b>Username:</b> " + name + "</p>" +
                "<p><b>Role:</b> " + role + "</p>" +
                "<p><b>Temporary Password:</b> " + tempPassword + "</p>" +
                "<p>Please log in and change your password immediately.</p>";
        sendEmail(toEmail, subject, htmlBody);
    }

    public void sendNewJobAlert(String toEmail, String name, String jobTitle, String company, String salary,
            String applyLink) throws IOException {
        String subject = "New Job Opportunity: " + jobTitle + " at " + company;
        String htmlContent = "<h3>Hello " + name + ",</h3>" +
                "<p>A new job has been posted that might interest you:</p>" +
                "<ul>" +
                "<li><b>Position:</b> " + jobTitle + "</li>" +
                "<li><b>Company:</b> " + company + "</li>" +
                "<li><b>Salary:</b> " + salary + "</li>" +
                "</ul>" +
                "<p><a href='" + applyLink
                + "' style='display: inline-block; padding: 10px 20px; background-color: #007bff; color: white; text-decoration: none; border-radius: 5px;'>Apply Now</a></p>"
                +
                "<br><p>Best regards,<br>Placement Portal Team</p>";
        sendEmail(toEmail, subject, htmlContent);
    }
}
