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
