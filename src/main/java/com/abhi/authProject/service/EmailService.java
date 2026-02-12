package com.abhi.authProject.service;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Value("${frontend.url}")
    private String frontendUrl;

    @Autowired
    private GlobalSettingsService globalSettingsService;

    @Autowired
    private ResendEmailService resendEmailService;

    public void sendEmail(String toEmail, String subject, String htmlContent) throws IOException {
        if (!globalSettingsService.isEmailAllowed()) {
            logger.info("Email sending is DISABLED (Master). Skipping email to: {}", toEmail);
            return;
        }

        try {
            logger.info("📧 Sending email via Resend to: {}", toEmail);
            resendEmailService.sendEmail(toEmail, subject, htmlContent);
            logger.info("✅ Email sent successfully via Resend to: {}", toEmail);
        } catch (Exception e) {
            logger.error("❌ Failed to send email via Resend to {}: {}", toEmail, e.getMessage());
            throw new IOException("Email sending failed: " + e.getMessage());
        }
    }

    public void sendPasswordResetEmail(String toEmail, String otp) throws IOException {
        String htmlContent = "<!DOCTYPE html>" +
                "<html><head><meta charset='UTF-8'></head>" +
                "<body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
                "<h2 style='color: #4F46E5;'>Password Reset Request</h2>" +
                "<p>Hello,</p>" +
                "<p>You requested to reset your password for the Placement Portal.</p>" +
                "<p>Your password reset OTP is:</p>" +
                "<div style='background-color: #f5f5f5; padding: 20px; border-radius: 8px; text-align: center; margin: 30px 0;'>"
                +
                "<h1 style='color: #4F46E5; font-size: 48px; letter-spacing: 8px; margin: 0;'>" + otp + "</h1>" +
                "</div>" +
                "<p style='color: #e53e3e; font-weight: bold;'>⏰ This OTP will expire in 15 minutes.</p>" +
                "<p>Enter this OTP on the password reset page to continue.</p>" +
                "<p>If you didn't request this, please ignore this email.</p>" +
                "<hr style='margin: 30px 0; border: none; border-top: 1px solid #ddd;'>" +
                "<p style='color: #666; font-size: 14px;'>Best regards,<br/>Placement Portal Team</p>" +
                "</div></body></html>";

        sendEmail(toEmail, "Password Reset OTP - Placement Portal", htmlContent);
    }

    public void sendPasswordResetConfirmation(String toEmail) throws IOException {
        if (!globalSettingsService.isAccountEmailAllowed()) {
            logger.info("Email sending (Account) is DISABLED. Skipping Password Reset Confirmation to {}", toEmail);
            return;
        }

        String htmlContent = "<h3>Password Reset Successful</h3>" +
                "<p>Hello,</p>" +
                "<p>Your password has been successfully reset.</p>" +
                "<p>If you didn't make this change, please contact support immediately.</p>" +
                "<br/>" +
                "<p>Best regards,<br/>Placement Portal Team</p>";

        sendEmail(toEmail, "Password Reset Successful - Placement Portal", htmlContent);
    }

    public void sendAcceptanceEmail(String toEmail, String studentName, String jobTitle,
            String companyName, String interviewDetails) throws IOException {
        if (!globalSettingsService.isStatusUpdateEmailAllowed()) {
            logger.info("Email sending (Status) is DISABLED. Skipping Acceptance Email to {}", toEmail);
            return;
        }

        String htmlContent = buildAcceptanceEmailHtml(studentName, jobTitle, companyName, interviewDetails);
        sendEmail(toEmail, "Congratulations! You've been shortlisted for " + jobTitle, htmlContent);
    }

    public void sendRejectionEmail(String toEmail, String studentName, String jobTitle, String companyName)
            throws IOException {
        if (!globalSettingsService.isStatusUpdateEmailAllowed()) {
            logger.info("Email sending (Status) is DISABLED. Skipping Rejection Email to {}", toEmail);
            return;
        }

        String htmlContent = buildRejectionEmailHtml(studentName, jobTitle, companyName);
        sendEmail(toEmail, "Application Status - " + jobTitle, htmlContent);
    }

    private String buildAcceptanceEmailHtml(String studentName, String jobTitle,
            String companyName, String interviewDetails) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html><head><style>");
        html.append("body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }");
        html.append(".container { max-width: 600px; margin: 0 auto; padding: 20px; }");
        html.append(
                ".header { background: linear-gradient(135deg, #4361ee, #00d9ff); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }");
        html.append(".content { padding: 30px; background: #f8f9fa; }");
        html.append(
                ".interview-round { margin: 15px 0; padding: 15px; background: white; border-left: 4px solid #4361ee; border-radius: 5px; }");
        html.append("</style></head><body>");
        html.append("<div class='container'>");
        html.append("<div class='header'><h1>🎉 Congratulations!</h1></div>");
        html.append("<div class='content'>");
        html.append("<p>Dear ").append(studentName).append(",</p>");
        html.append(
                "<p>We are pleased to inform you that you have been <strong>shortlisted</strong> for the position of <strong>")
                .append(jobTitle).append("</strong> at <strong>").append(companyName).append("</strong>.</p>");

        if (interviewDetails != null && !interviewDetails.isEmpty()) {
            html.append(formatInterviewDetailsHtml(interviewDetails));
        }

        html.append("<p>Please check your Interview section in the portal for complete details.</p>");
        html.append("<p>Best regards,<br>Placement Cell</p>");
        html.append("</div></div></body></html>");

        return html.toString();
    }

    private String buildRejectionEmailHtml(String studentName, String jobTitle, String companyName) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html><head><style>");
        html.append("body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }");
        html.append(".container { max-width: 600px; margin: 0 auto; padding: 20px; }");
        html.append(
                ".header { background: #6c757d; color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }");
        html.append(".content { padding: 30px; background: #f8f9fa; }");
        html.append("</style></head><body>");
        html.append("<div class='container'>");
        html.append("<div class='header'><h1>Application Status Update</h1></div>");
        html.append("<div class='content'>");
        html.append("<p>Dear ").append(studentName).append(",</p>");
        html.append("<p>Thank you for applying for the position of <strong>")
                .append(jobTitle).append("</strong> at <strong>").append(companyName).append("</strong>.</p>");
        html.append(
                "<p>After careful consideration, we regret to inform you that we are unable to proceed with your application at this time.</p>");
        html.append("<p>We encourage you to apply for other opportunities available on our portal.</p>");
        html.append("<p>Best regards,<br>Placement Cell</p>");
        html.append("</div></div></body></html>");

        return html.toString();
    }

    private String formatInterviewDetailsHtml(String interviewDetailsJson) {
        StringBuilder html = new StringBuilder();
        try {
            JSONObject details = new JSONObject(interviewDetailsJson);
            html.append("<div style='margin: 20px 0;'>");
            html.append("<h3 style='color: #4361ee;'>Interview Schedule:</h3>");
            // Add interview details parsing logic here
            html.append("</div>");
        } catch (Exception e) {
            logger.error("Error formatting interview details: {}", e.getMessage());
            html.append("<p>Interview details will be available in your portal.</p>");
        }
        return html.toString();
    }

    public void sendNewJobAlert(String toEmail, String studentName, String jobTitle, String companyName, String salary,
            String applyLink) {
        if (!globalSettingsService.isNewJobEmailAllowed()) {
            logger.info("Email sending (New Job) is DISABLED. Skipping New Job Alert to {}", toEmail);
            return;
        }
        try {
            String htmlContent = "<h2>New Job Alert!</h2><p>Role: " + jobTitle + "</p><p>Company: " + companyName
                    + "</p>";
            if (salary != null)
                htmlContent += "<p>Salary: " + salary + "</p>";
            htmlContent += "<a href='" + applyLink + "'>Apply Now</a>";
            sendEmail(toEmail, "🚀 New Job Alert: " + jobTitle + " at " + companyName, htmlContent);
        } catch (IOException e) {
            logger.error("Error sending job alert: {}", e.getMessage());
        }
    }

    public void sendStatusUpdateEmail(String toEmail, String studentName, String jobTitle, String companyName,
            String status) {
        if (!globalSettingsService.isStatusUpdateEmailAllowed()) {
            logger.info("Email sending (Status) is DISABLED. Skipping Status Update Email to {}", toEmail);
            return;
        }
        try {
            String htmlContent = "<h2>Application Update</h2><p>Dear " + studentName
                    + ", your application for " + jobTitle + " has been updated to: <strong>" + status
                    + "</strong></p>";
            sendEmail(toEmail, "Update on your application: " + jobTitle, htmlContent);
        } catch (IOException e) {
            logger.error("Error sending status update: {}", e.getMessage());
        }
    }

    public void sendAccountCreatedEmail(String toEmail, String username, String role, String password) {
        if (!globalSettingsService.isAccountEmailAllowed()) {
            logger.info("Email sending (Account) is DISABLED. Skipping Account Created Email to {}", toEmail);
            return;
        }
        try {
            String htmlContent = "<h2>Account Created</h2><p>Username: " + username + "</p><p>Password: " + password
                    + "</p>";
            sendEmail(toEmail, "Welcome to Placement Portal - Account Created", htmlContent);
        } catch (IOException e) {
            logger.error("Error sending welcome email: {}", e.getMessage());
        }
    }

    public void sendShortlistedEmail(String toEmail, String studentName, String jobTitle, String companyName,
            String interviewDate, String interviewLocation) throws IOException {
        String htmlContent = "<h2>🎉 Congratulations!</h2><p>Dear " + studentName + ", you have been shortlisted for "
                + jobTitle + " at " + companyName + ".</p>";
        htmlContent += "<p>Date: " + interviewDate + "</p><p>Location: " + interviewLocation + "</p>";
        sendEmail(toEmail, "🎉 You've Been Shortlisted!", htmlContent);
    }

    public void sendSelectedEmail(String toEmail, String studentName, String jobTitle, String companyName)
            throws IOException {
        String htmlContent = "<h2>🎊 Excellent News!</h2><p>Dear " + studentName + ", you have been selected for "
                + jobTitle + " at " + companyName + "!</p>";
        sendEmail(toEmail, "🎊 Selection Update - " + jobTitle, htmlContent);
    }

    public void sendRejectedEmail(String toEmail, String studentName, String jobTitle, String companyName)
            throws IOException {
        sendRejectionEmail(toEmail, studentName, jobTitle, companyName);
    }

    public void sendAccountUpgradeConfirmation(String toEmail, String computerCode, String name) throws IOException {
        if (!globalSettingsService.isAccountEmailAllowed()) {
            logger.info("Email sending (Account) is DISABLED. Skipping Account Upgrade Email to {}", toEmail);
            return;
        }
        String htmlContent = "<h2>✅ Account Upgraded Successfully!</h2><p>Dear " + (name != null ? name : "User")
                + ",</p>";
        htmlContent += "<p>Your account has been successfully recovered and upgraded to the new system!</p>";
        htmlContent += "<p><strong>Computer Code:</strong> " + computerCode + "</p>";
        sendEmail(toEmail, "✅ Account Successfully Upgraded - Placement Portal", htmlContent);
    }

    public void sendEmailWithAttachment(String toEmail, String subject, String htmlContent, byte[] attachment)
            throws IOException {
        if (!globalSettingsService.isEmailAllowed()) {
            logger.info("Email sending is DISABLED (Master). Skipping Email with Attachment to {}", toEmail);
            return;
        }
        resendEmailService.sendEmail(toEmail, subject, htmlContent); // Resend simplified for now
    }

    public void sendEmailWithAttachment(String toEmail, String subject, String htmlContent, byte[] attachment,
            String filename) throws IOException {
        if (!globalSettingsService.isEmailAllowed()) {
            logger.info("Email sending is DISABLED (Master). Skipping Email with Attachment to {}", toEmail);
            return;
        }
        resendEmailService.sendEmailWithAttachment(toEmail, subject, htmlContent, attachment, filename);
    }

    // ADDED: Compatibility method for String paths (like from local storage)
    public void sendEmailWithLocalFile(String toEmail, String subject, String htmlContent, String filePath)
            throws IOException {
        if (!globalSettingsService.isEmailAllowed()) {
            logger.info("Email sending is DISABLED (Master). Skipping Email with Attachment to {}", toEmail);
            return;
        }
        try {
            java.nio.file.Path path = java.nio.file.Paths.get(filePath);
            byte[] fileContent = java.nio.file.Files.readAllBytes(path);
            String fileName = path.getFileName().toString();
            resendEmailService.sendEmailWithAttachment(toEmail, subject, htmlContent, fileContent, fileName);
        } catch (Exception e) {
            logger.error("Error reading attachment from path: {}. Sending without attachment.", filePath);
            resendEmailService.sendEmail(toEmail, subject, htmlContent);
        }
    }
}
