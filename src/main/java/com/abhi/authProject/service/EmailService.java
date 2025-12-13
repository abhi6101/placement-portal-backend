package com.abhi.authProject.service;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class EmailService {

    @Value("${sendgrid.api.key}")
    private String sendGridApiKey;

    @Value("${sendgrid.from.email:hack2hired.official@gmail.com}")
    private String fromEmail;

    public void sendPasswordResetEmail(String toEmail, String resetToken) throws IOException {
        String resetUrl = "https://hack-2-hired.onrender.com/reset-password?token=" + resetToken;

        Email from = new Email(fromEmail);
        Email to = new Email(toEmail);
        String subject = "Password Reset Request - Placement Portal";

        Content content = new Content("text/html",
                "<h3>Password Reset Request</h3>" +
                        "<p>Hello,</p>" +
                        "<p>You requested to reset your password for the Placement Portal.</p>" +
                        "<p>Click the button below to reset your password:</p>" +
                        "<div style='margin: 20px 0;'>" +
                        "  <a href='" + resetUrl
                        + "' style='background-color: #4F46E5; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; display: inline-block;'>Reset Password</a>"
                        +
                        "</div>" +
                        "<p>Or copy this link: <a href='" + resetUrl + "'>" + resetUrl + "</a></p>" +
                        "<p><strong>This link will expire in 1 hour.</strong></p>" +
                        "<p>If you didn't request this, please ignore this email.</p>" +
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
}
