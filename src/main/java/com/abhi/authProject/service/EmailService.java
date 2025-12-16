package com.abhi.authProject.service;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Value("${sendgrid.api.key}")
    private String sendGridApiKey;

    @Value("${sendgrid.from.email:hack2hired.official@gmail.com}")
    private String fromEmail;

    public void sendPasswordResetEmail(String toEmail, String otp) throws IOException {
        Email from = new Email(fromEmail);
        Email to = new Email(toEmail);
        String subject = "Password Reset OTP - Placement Portal";

        String htmlContent = "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset='UTF-8'></head>" +
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
                "</div>" +
                "</body>" +
                "</html>";

        Content content = new Content("text/html", htmlContent);
        Mail mail = new Mail(from, subject, to, content);
        SendGrid sg = new SendGrid(sendGridApiKey);
        Request request = new Request();

        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        request.setBody(mail.build());

        Response response = sg.api(request);

        if (response.getStatusCode() >= 400) {
            throw new IOException("Failed to send email: " + response.getBody());
        }
    }

    public void sendEmail(String toEmail, String subject, String body) throws IOException {
        Email from = new Email(fromEmail);
        Email to = new Email(toEmail);
        Content content = new Content("text/plain", body);
        Mail mail = new Mail(from, subject, to, content);

        SendGrid sg = new SendGrid(sendGridApiKey);
        Request request = new Request();
        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        request.setBody(mail.build());

        Response response = sg.api(request);
        if (response.getStatusCode() >= 400) {
            throw new IOException("Failed to send email: " + response.getBody());
        }
    }

    public void sendPasswordResetConfirmation(String toEmail) throws IOException {
        Email from = new Email(fromEmail);
        Email to = new Email(toEmail);
        String subject = "Password Reset Successful - Placement Portal";

        Content content = new Content("text/html",
                "<h3>Password Reset Successful</h3>" +
                        "<p>Hello,</p>" +
                        "<p>Your password has been successfully reset.</p>" +
                        "<p>If you didn't make this change, please contact support immediately.</p>" +
                        "<br/>" +
                        "<p>Best regards,<br/>Placement Portal Team</p>");

        Mail mail = new Mail(from, subject, to, content);
        SendGrid sg = new SendGrid(sendGridApiKey);
        Request request = new Request();

        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        request.setBody(mail.build());

        Response response = sg.api(request);

        if (response.getStatusCode() >= 400) {
            throw new IOException("Failed to send email: " + response.getBody());
        }
    }

    /**
     * Send acceptance email to student when application is shortlisted
     */
    public void sendAcceptanceEmail(String toEmail, String studentName, String jobTitle,
            String companyName, String interviewDetails) throws IOException {
        Email from = new Email(fromEmail);
        Email to = new Email(toEmail);
        String subject = "Congratulations! You've been shortlisted for " + jobTitle;

        String htmlContent = buildAcceptanceEmailHtml(studentName, jobTitle, companyName, interviewDetails);
        Content content = new Content("text/html", htmlContent);

        Mail mail = new Mail(from, subject, to, content);
        SendGrid sg = new SendGrid(sendGridApiKey);
        Request request = new Request();

        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        request.setBody(mail.build());

        Response response = sg.api(request);

        if (response.getStatusCode() >= 400) {
            logger.error("Failed to send acceptance email. Status: {}", response.getStatusCode());
            throw new IOException("Failed to send acceptance email: " + response.getBody());
        }
        logger.info("Acceptance email sent successfully to: {}", toEmail);
    }

    /**
     * Send rejection email to student when application is rejected
     */
    public void sendRejectionEmail(String toEmail, String studentName, String jobTitle, String companyName)
            throws IOException {
        Email from = new Email(fromEmail);
        Email to = new Email(toEmail);
        String subject = "Application Status - " + jobTitle;

        String htmlContent = buildRejectionEmailHtml(studentName, jobTitle, companyName);
        Content content = new Content("text/html", htmlContent);

        Mail mail = new Mail(from, subject, to, content);
        SendGrid sg = new SendGrid(sendGridApiKey);
        Request request = new Request();

        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        request.setBody(mail.build());

        Response response = sg.api(request);

        if (response.getStatusCode() >= 400) {
            logger.error("Failed to send rejection email. Status: {}", response.getStatusCode());
            throw new IOException("Failed to send rejection email: " + response.getBody());
        }
        logger.info("Rejection email sent successfully to: {}", toEmail);
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

            if (details.has("codingRound")) {
                JSONObject coding = details.getJSONObject("codingRound");
                if (coding.optBoolean("enabled", false)) {
                    html.append("<div class='interview-round'>");
                    html.append("<h4>📝 Coding Round</h4>");
                    html.append("<p><strong>Date:</strong> ").append(coding.optString("date", "TBD")).append("</p>");
                    html.append("<p><strong>Time:</strong> ").append(coding.optString("time", "TBD")).append("</p>");
                    html.append("<p><strong>Venue:</strong> ").append(coding.optString("venue", "TBD")).append("</p>");
                    if (coding.has("instructions")) {
                        html.append("<p><strong>Instructions:</strong> ").append(coding.getString("instructions"))
                                .append("</p>");
                    }
                    html.append("</div>");
                }
            }

            if (details.has("technicalInterview")) {
                JSONObject technical = details.getJSONObject("technicalInterview");
                if (technical.optBoolean("enabled", false)) {
                    html.append("<div class='interview-round' style='border-left-color: #06ffa5;'>");
                    html.append("<h4>💼 Technical Interview</h4>");
                    html.append("<p><strong>Date:</strong> ").append(technical.optString("date", "TBD")).append("</p>");
                    html.append("<p><strong>Time:</strong> ").append(technical.optString("time", "TBD")).append("</p>");
                    html.append("<p><strong>Venue:</strong> ").append(technical.optString("venue", "TBD"))
                            .append("</p>");
                    if (technical.has("topics")) {
                        html.append("<p><strong>Topics:</strong> ").append(technical.getString("topics"))
                                .append("</p>");
                    }
                    html.append("</div>");
                }
            }

            if (details.has("hrRound")) {
                JSONObject hr = details.getJSONObject("hrRound");
                if (hr.optBoolean("enabled", false)) {
                    html.append("<div class='interview-round' style='border-left-color: #f72585;'>");
                    html.append("<h4>👥 HR Round</h4>");
                    html.append("<p><strong>Date:</strong> ").append(hr.optString("date", "TBD")).append("</p>");
                    html.append("<p><strong>Time:</strong> ").append(hr.optString("time", "TBD")).append("</p>");
                    html.append("<p><strong>Venue:</strong> ").append(hr.optString("venue", "TBD")).append("</p>");
                    html.append("</div>");
                }
            }

            if (details.has("projectTask")) {
                JSONObject project = details.getJSONObject("projectTask");
                if (project.optBoolean("enabled", false)) {
                    html.append(
                            "<div class='interview-round' style='background: #fff3cd; border-left-color: #ffc107;'>");
                    html.append("<h4>🎯 Optional Project Task</h4>");
                    html.append("<p><strong>Description:</strong> ").append(project.optString("description", "TBD"))
                            .append("</p>");
                    html.append("<p><strong>Deadline:</strong> ").append(project.optString("deadline", "24"))
                            .append(" hours</p>");
                    if (project.has("requirements")) {
                        html.append("<p><strong>Requirements:</strong> ").append(project.getString("requirements"))
                                .append("</p>");
                    }
                    html.append("</div>");
                }
            }
            html.append("</div>");
        } catch (Exception e) {
            logger.error("Error formatting interview details: {}", e.getMessage());
            html.append("<p>Interview details will be available in your portal.</p>");
        }
        return html.toString();
    }

    public void sendNewJobAlert(String toEmail, String studentName, String jobTitle, String companyName, String salary,
            String applyLink) {
        try {
            Email from = new Email(fromEmail);
            Email to = new Email(toEmail);
            String subject = "🚀 New Job Alert: " + jobTitle + " at " + companyName;

            // 1. Create Plain Text Content (Best Practice for Anti-Spam)
            String plainText = "Hello " + studentName + ",\n\n" +
                    "A new job opportunity is available!\n" +
                    "Role: " + jobTitle + "\n" +
                    "Company: " + companyName + "\n" +
                    (salary != null ? "Salary: " + salary + "\n" : "") +
                    "\nApply here: " + (applyLink != null ? applyLink : "https://hack-2-hired.onrender.com/jobs")
                    + "\n\n" +
                    "Best,\nPlacement Portal Team";
            Content textContent = new Content("text/plain", plainText);

            // 2. Create HTML Content
            String htmlContent = buildNewJobEmailHtml(studentName, jobTitle, companyName, salary, applyLink);
            Content htmlContentObj = new Content("text/html", htmlContent);

            // 3. Add Both (Multipart)
            Mail mail = new Mail(from, subject, to, textContent);
            mail.addContent(htmlContentObj);

            SendGrid sg = new SendGrid(sendGridApiKey);
            Request request = new Request();

            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sg.api(request);
            if (response.getStatusCode() >= 400) {
                logger.error("Failed to send Job Alert to {}. Status: {}", toEmail, response.getStatusCode());
            } else {
                logger.info("Job Alert sent to {}", toEmail);
            }
        } catch (IOException e) {
            logger.error("Error sending Job Alert to {}: {}", toEmail, e.getMessage());
        }
    }

    public void sendStatusUpdateEmail(String toEmail, String studentName, String jobTitle, String companyName,
            String status) {
        try {
            Email from = new Email(fromEmail);
            Email to = new Email(toEmail);
            String subject = "Update on your application: " + jobTitle + " at " + companyName;

            String color = "#4361ee"; // Default Blue
            String statusText = status;

            if ("SHORTLISTED".equalsIgnoreCase(status)) {
                color = "#22c55e"; // Green
                statusText = "SHORTLISTED";
            } else if ("SELECTED".equalsIgnoreCase(status)) {
                color = "#4cccff"; // Cyan/Blue
                statusText = "SELECTED";
            } else if ("REJECTED".equalsIgnoreCase(status)) {
                color = "#ef4444"; // Red
                statusText = "NOT SELECTED";
            }

            // Simple HTML Template
            String htmlContent = "<!DOCTYPE html><html><body style='font-family: Arial, sans-serif; color: #333;'>" +
                    "<div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #eee; border-radius: 8px;'>"
                    +
                    "<h2 style='color: " + color + ";'>Application Status Update</h2>" +
                    "<p>Dear " + studentName + ",</p>" +
                    "<p>Your application for <strong>" + jobTitle + "</strong> at <strong>" + companyName
                    + "</strong> has been updated to:</p>" +
                    "<h1 style='color: " + color
                    + "; text-align: center; background: #f9f9f9; padding: 15px; border-radius: 8px;'>" + statusText
                    + "</h1>" +
                    "<p>Please login to the portal for more details.</p>" +
                    "<br/><p>Best regards,<br/>Placement Portal Team</p>" +
                    "</div></body></html>";

            Content content = new Content("text/html", htmlContent);
            Mail mail = new Mail(from, subject, to, content);
            SendGrid sg = new SendGrid(sendGridApiKey);
            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            SendGrid sg2 = new SendGrid(sendGridApiKey);
            Response response = sg2.api(request);

            if (response.getStatusCode() >= 400) {
                logger.error("Failed to send status email to {}. Status: {}", toEmail, response.getStatusCode());
            } else {
                logger.info("Status email sent to {}", toEmail);
            }
        } catch (IOException e) {
            logger.error("Error sending status email: {}", e.getMessage());
        }
    }

    public void sendAccountCreatedEmail(String toEmail, String username, String role, String password) {
        try {
            Email from = new Email(fromEmail);
            Email to = new Email(toEmail);
            String subject = "Welcome to Placement Portal - Account Created";

            String htmlContent = "<!DOCTYPE html><html><body style='font-family: Arial, sans-serif; color: #333;'>" +
                    "<div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #eee; border-radius: 8px;'>"
                    +
                    "<h2 style='color: #4f46e5;'>Welcome to the Team!</h2>" +
                    "<p>Your account has been successfully created on the Placement Portal.</p>" +
                    "<div style='background: #f9f9f9; padding: 15px; border-radius: 8px; margin: 20px 0;'>" +
                    "<p><strong>Username:</strong> " + username + "</p>" +
                    "<p><strong>Role:</strong> " + role + "</p>" +
                    "<p><strong>Password:</strong> " + password + "</p>" +
                    "</div>" +
                    "<p>Please login and change your password immediately.</p>" +
                    "<br/>" +
                    "<p>Best regards,<br/>Placement Portal Team</p>" +
                    "</div></body></html>";

            Content content = new Content("text/html", htmlContent);
            Mail mail = new Mail(from, subject, to, content);

            SendGrid sg = new SendGrid(sendGridApiKey);
            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sg.api(request);

            if (response.getStatusCode() >= 400) {
                logger.error("Failed to send welcome email to {}. Status: {}", toEmail, response.getStatusCode());
            } else {
                logger.info("Welcome email sent to {}", toEmail);
            }
        } catch (IOException e) {
            logger.error("Error sending welcome email to {}: {}", toEmail, e.getMessage());
        }
    }

    private String buildNewJobEmailHtml(String studentName, String jobTitle, String companyName, String salary,
            String applyLink) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html><head><style>");
        html.append(
                "body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; background-color: #f4f6f8; margin: 0; padding: 0; }");
        html.append(
                ".container { max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }");
        html.append(
                ".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 40px 20px; text-align: center; }");
        html.append(".header h1 { margin: 0; font-size: 28px; font-weight: 700; letter-spacing: 1px; }");
        html.append(".content { padding: 40px 30px; }");
        html.append(
                ".job-card { background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 25px; margin: 20px 0; text-align: center; }");
        html.append(".job-title { color: #2d3748; font-size: 22px; font-weight: 700; margin-bottom: 5px; }");
        html.append(".company-name { color: #4a5568; font-size: 18px; font-weight: 500; margin-bottom: 15px; }");
        html.append(
                ".salary-tag { display: inline-block; background-color: #e6fffa; color: #047481; padding: 5px 12px; border-radius: 20px; font-size: 14px; font-weight: 600; }");
        html.append(
                ".btn-apply { display: inline-block; background-color: #48bb78; color: white; padding: 12px 30px; text-decoration: none; border-radius: 50px; font-weight: bold; margin-top: 25px; transition: background-color 0.3s; }");
        html.append(
                ".footer { background-color: #edf2f7; padding: 20px; text-align: center; color: #718096; font-size: 12px; }");
        html.append("</style></head><body>");

        html.append("<div class='container'>");
        html.append("<div class='header'>");
        html.append("<h1>New Opportunity Details!</h1>");
        html.append("</div>");

        html.append("<div class='content'>");
        html.append("<p style='font-size: 16px;'>Hello <strong>").append(studentName).append("</strong>,</p>");
        html.append(
                "<p style='font-size: 16px; color: #4a5568;'>An exciting new placement opportunity has just arrived on the portal. Check it out!</p>");

        html.append("<div class='job-card'>");
        html.append("<div class='job-title'>").append(jobTitle).append("</div>");
        html.append("<div class='company-name'>").append(companyName).append("</div>");
        if (salary != null && !salary.isEmpty() && !salary.equals("0")) {
            html.append("<div><span class='salary-tag'>💰 Salary: ₹").append(salary).append("</span></div>");
        }
        html.append("<br/>");
        html.append("<a href='").append(
                applyLink != null && !applyLink.isEmpty() ? applyLink : "https://hack-2-hired.onrender.com/jobs")
                .append("' class='btn-apply'>View & Apply</a>");
        html.append("</div>");

        html.append(
                "<p style='text-align: center; color: #718096; margin-top: 30px;'>Don't wait! Applications might close soon.</p>");
        html.append("</div>");

        html.append("<div class='footer'>");
        html.append("<p>&copy; 2025 Placement Portal. All rights reserved.</p>");
        html.append("</div>");
        html.append("</div>");

        html.append("</body></html>");
        return html.toString();
    }

    // SHORTLISTED - Send interview details
    public void sendShortlistedEmail(String toEmail, String studentName, String jobTitle, String companyName,
            String interviewDate, String interviewLocation) throws IOException {
        Email from = new Email(fromEmail);
        Email to = new Email(toEmail);
        String subject = "🎉 You've Been Shortlisted for " + jobTitle + " at " + companyName + "!";

        String htmlContent = "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset='UTF-8'></head>" +
                "<body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); border-radius: 10px;'>"
                +
                "<div style='background: white; padding: 30px; border-radius: 8px;'>" +
                "<h2 style='color: #667eea; text-align: center;'>🎉 Congratulations!</h2>" +
                "<p>Dear " + studentName + ",</p>" +
                "<p><strong>Great news!</strong> You have been <span style='color: #22c55e; font-weight: bold;'>SHORTLISTED</span> for the position of <strong>"
                + jobTitle + "</strong> at <strong>" + companyName + "</strong>.</p>" +
                "<div style='background-color: #f0f9ff; padding: 20px; border-left: 4px solid #667eea; margin: 20px 0;'>"
                +
                "<h3 style='color: #667eea; margin-top: 0;'>📅 Interview Details:</h3>" +
                "<p style='margin: 10px 0;'><strong>Date:</strong> " + (interviewDate != null ? interviewDate : "TBA")
                + "</p>" +
                "<p style='margin: 10px 0;'><strong>Location:</strong> "
                + (interviewLocation != null ? interviewLocation : "TBA") + "</p>" +
                "<p style='margin: 10px 0;'><strong>Company:</strong> " + companyName + "</p>" +
                "</div>" +
                "<p style='color: #4F46E5; font-weight: bold;'>💡 Please be prepared and arrive on time. Good luck!</p>"
                +
                "<hr style='margin: 30px 0; border: none; border-top: 1px solid #ddd;'>" +
                "<p style='color: #666; font-size: 14px;'>Best regards,<br/>Placement Portal Team</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";

        Content content = new Content("text/html", htmlContent);
        Mail mail = new Mail(from, subject, to, content);

        SendGrid sg = new SendGrid(sendGridApiKey);
        Request request = new Request();
        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        request.setBody(mail.build());
        Response response = sg.api(request);

        logger.info("Shortlisted email sent to: {} - Status: {}", toEmail, response.getStatusCode());
    }

    // SELECTED - Congratulations only (no interview)
    public void sendSelectedEmail(String toEmail, String studentName, String jobTitle, String companyName)
            throws IOException {
        Email from = new Email(fromEmail);
        Email to = new Email(toEmail);
        String subject = "🎊 Congratulations! You've Been Selected for " + jobTitle + "!";

        String htmlContent = "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset='UTF-8'></head>" +
                "<body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px; background: linear-gradient(135deg, #06ffa5 0%, #00d9ff 100%); border-radius: 10px;'>"
                +
                "<div style='background: white; padding: 30px; border-radius: 8px;'>" +
                "<h2 style='color: #06ffa5; text-align: center;'>🎊 Congratulations!</h2>" +
                "<p>Dear " + studentName + ",</p>" +
                "<p><strong>Excellent news!</strong> You have been <span style='color: #06ffa5; font-weight: bold;'>SELECTED</span> for the position of <strong>"
                + jobTitle + "</strong> at <strong>" + companyName + "</strong>!</p>" +
                "<div style='background-color: #f0fdf4; padding: 20px; border-left: 4px solid #06ffa5; margin: 20px 0;'>"
                +
                "<p style='margin: 0;'>🌟 This is a significant achievement! Further details regarding onboarding and next steps will be shared with you soon.</p>"
                +
                "</div>" +
                "<p style='color: #059669; font-weight: bold;'>We are proud of your accomplishment and wish you all the best in your new role!</p>"
                +
                "<hr style='margin: 30px 0; border: none; border-top: 1px solid #ddd;'>" +
                "<p style='color: #666; font-size: 14px;'>Best regards,<br/>Placement Portal Team</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";

        Content content = new Content("text/html", htmlContent);
        Mail mail = new Mail(from, subject, to, content);

        SendGrid sg = new SendGrid(sendGridApiKey);
        Request request = new Request();
        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        request.setBody(mail.build());
        Response response = sg.api(request);

        logger.info("Selected email sent to: {} - Status: {}", toEmail, response.getStatusCode());
    }

    // REJECTED - Polite rejection
    public void sendRejectedEmail(String toEmail, String studentName, String jobTitle, String companyName)
            throws IOException {
        Email from = new Email(fromEmail);
        Email to = new Email(toEmail);
        String subject = "Update on Your Application for " + jobTitle;

        String htmlContent = "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset='UTF-8'></head>" +
                "<body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
                "<div style='background: white; padding: 30px; border-radius: 8px; border: 1px solid #e5e7eb;'>" +
                "<h2 style='color: #4F46E5;'>Application Update</h2>" +
                "<p>Dear " + studentName + ",</p>" +
                "<p>Thank you for your interest in the <strong>" + jobTitle + "</strong> position at <strong>"
                + companyName + "</strong>.</p>" +
                "<p>After careful consideration, we regret to inform you that we will not be moving forward with your application at this time.</p>"
                +
                "<div style='background-color: #fef3c7; padding: 15px; border-left: 4px solid #f59e0b; margin: 20px 0;'>"
                +
                "<p style='margin: 0;'>💪 We encourage you to continue applying for other opportunities. Your skills and experience are valuable, and we wish you the best in your job search.</p>"
                +
                "</div>" +
                "<p>Thank you for your time and interest in our placement portal.</p>" +
                "<hr style='margin: 30px 0; border: none; border-top: 1px solid #ddd;'>" +
                "<p style='color: #666; font-size: 14px;'>Best wishes,<br/>Placement Portal Team</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";

        Content content = new Content("text/html", htmlContent);
        Mail mail = new Mail(from, subject, to, content);

        SendGrid sg = new SendGrid(sendGridApiKey);
        Request request = new Request();
        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        request.setBody(mail.build());

        Response response = sg.api(request);

        logger.info("Rejected email sent to: {} - Status: {}", toEmail, response.getStatusCode());
    }
}
