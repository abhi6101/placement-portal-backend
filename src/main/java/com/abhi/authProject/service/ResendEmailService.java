package com.abhi.authProject.service;

import com.resend.*;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class ResendEmailService {

    private static final Logger logger = LoggerFactory.getLogger(ResendEmailService.class);

    @Value("${resend.api.key}")
    private String apiKey;

    @Value("${resend.from.email:onboarding@resend.dev}")
    private String fromEmail;

    @Value("${resend.from.name:Hack2Hired Team}")
    private String fromName;

    /**
     * Send an email using Resend API
     */
    public void sendEmail(String toEmail, String subject, String htmlContent) throws IOException {
        try {
            // Log API credentials (masked)
            logger.info("🔑 Resend API Key: {}...",
                    apiKey != null ? apiKey.substring(0, Math.min(8, apiKey.length())) : "NULL");
            logger.info("📧 From Email: {}", fromEmail);
            logger.info("📧 From Name: {}", fromName);

            // Create Resend client
            Resend resend = new Resend(apiKey);
            logger.info("✅ Resend client created successfully");

            // Build email parameters
            SendEmailRequest emailRequest = SendEmailRequest.builder()
                    .from(fromName + " <" + fromEmail + ">")
                    .to(toEmail)
                    .subject(subject)
                    .html(htmlContent)
                    .build();

            logger.info("📤 Email payload: From={}, To={}, Subject={}", fromEmail, toEmail, subject);
            logger.info("🚀 Sending request to Resend API...");

            // Send email
            SendEmailResponse response = resend.emails().send(emailRequest);

            logger.info("📥 Resend API Response - Email ID: {}", response.getId());
            logger.info("✅ Email successfully sent to {}. Email ID: {}", toEmail, response.getId());

        } catch (ResendException e) {
            logger.error("❌ Resend API error: Status={}, Message={}", e.statusCode(), e.getMessage());
            e.printStackTrace();
            throw new IOException("Resend API error: " + e.getMessage());
        } catch (Exception e) {
            logger.error("❌ Exception while sending email to {}: {}", toEmail, e.getMessage());
            e.printStackTrace();
            throw new IOException("Email sending error: " + e.getMessage());
        }
    }

    /**
     * Send an email with attachments
     */
    public void sendEmailWithAttachment(String toEmail, String subject, String htmlContent, byte[] attachment,
            String filename) throws IOException {
        try {
            logger.info("📎 Preparing email with attachment '{}' to: {}", filename, toEmail);

            Resend resend = new Resend(apiKey);

            // Create the attachment object if provided
            SendEmailRequest.Attachment resendAttachment = null;
            if (attachment != null && attachment.length > 0) {
                resendAttachment = SendEmailRequest.Attachment.builder()
                        .fileName(filename != null ? filename : "attachment.pdf")
                        .content(attachment)
                        .build();
            }

            // Build email request
            SendEmailRequest.Builder builder = SendEmailRequest.builder()
                    .from(fromName + " <" + fromEmail + ">")
                    .to(toEmail)
                    .subject(subject)
                    .html(htmlContent);

            if (resendAttachment != null) {
                builder.attachments(resendAttachment);
            }

            SendEmailRequest emailRequest = builder.build();

            logger.info("🚀 Sending request to Resend API (with attachment)...");
            SendEmailResponse response = resend.emails().send(emailRequest);

            if (response != null && response.getId() != null) {
                logger.info("✅ Email with attachment sent successfully. Email ID: {}", response.getId());
            } else {
                logger.warn("⚠️ Email sent but no ID was returned from Resend.");
            }

        } catch (ResendException e) {
            logger.error("❌ Resend API error with attachment: Status={}, Message={}", e.statusCode(), e.getMessage());
            throw new IOException("Resend error: " + e.getMessage());
        } catch (Exception e) {
            logger.error("❌ Unexpected error sending email with attachment: {}", e.getMessage());
            throw new IOException("Email sending failed: " + e.getMessage());
        }
    }
}
