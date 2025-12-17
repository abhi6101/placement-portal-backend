package com.abhi.authProject.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Attachments;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;

@Service
public class SendGridEmailService {

    private static final Logger logger = LoggerFactory.getLogger(SendGridEmailService.class);

    @Value("${sendgrid.api.key}")
    private String sendGridApiKey;

    @Value("${SENDER_FROM_EMAIL}")
    private String fromEmail;

    @org.springframework.beans.factory.annotation.Autowired
    private GlobalSettingsService globalSettingsService;

    /**
     * Sends an email with an optional file attachment.
     * If attachmentPath is null or empty, it sends a simple email.
     */
    public void sendEmailWithAttachment(String toEmail, String subject, String htmlContent, String attachmentPath)
            throws IOException {
        if (!globalSettingsService.isEmailAllowed()) {
            logger.info("Email sending is DISABLED (Master). Skipping email (with attachment) to: {}", toEmail);
            return;
        }
        Email from = new Email(fromEmail);
        Email to = new Email(toEmail);
        Content content = new Content("text/html", htmlContent);
        Mail mail = new Mail(from, subject, to, content);

        // If an attachment path is provided, read the file and attach it
        if (attachmentPath != null && !attachmentPath.isEmpty()) {
            try {
                Path path = Paths.get(attachmentPath);
                byte[] fileBytes = Files.readAllBytes(path);
                String base64Content = Base64.getEncoder().encodeToString(fileBytes);

                Attachments attachments = new Attachments();
                attachments.setContent(base64Content);
                attachments.setFilename(path.getFileName().toString());
                attachments.setType(Files.probeContentType(path)); // Auto-detects file type (e.g., "application/pdf")
                attachments.setDisposition("attachment");
                mail.addAttachments(attachments);
                logger.info("Successfully attached file: {}", path.getFileName().toString());
            } catch (IOException e) {
                logger.error("Could not read or attach file: {}. Email will be sent without it.", attachmentPath, e);
            }
        }

        SendGrid sg = new SendGrid(sendGridApiKey);
        Request request = new Request();

        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);

            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                logger.info("Email successfully sent to {}. Status Code: {}", toEmail, response.getStatusCode());
            } else {
                // Throw an exception to let the calling service know it failed
                throw new IOException("Failed to send email via SendGrid. Status: " + response.getStatusCode()
                        + " Body: " + response.getBody());
            }
        } catch (IOException ex) {
            logger.error("IO Exception while sending SendGrid email to {}: {}", toEmail, ex.getMessage());
            throw ex; // Re-throw the exception
        }
    }
}