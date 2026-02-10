package com.abhi.authProject.service;

import com.mailjet.client.ClientOptions;
import com.mailjet.client.MailjetClient;
import com.mailjet.client.MailjetRequest;
import com.mailjet.client.MailjetResponse;
import com.mailjet.client.resource.Emailv31;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

@Service
public class MailjetEmailService {

    private static final Logger logger = LoggerFactory.getLogger(MailjetEmailService.class);

    @Value("${mailjet.api.key}")
    private String apiKey;

    @Value("${mailjet.secret.key}")
    private String secretKey;

    @Value("${mailjet.from.email}")
    private String fromEmail;

    @Value("${mailjet.from.name}")
    private String fromName;

    @Autowired
    private GlobalSettingsService globalSettingsService;

    private MailjetClient getMailjetClient() {
        return new MailjetClient(
                ClientOptions.builder()
                        .apiKey(apiKey)
                        .apiSecretKey(secretKey)
                        .build());
    }

    /**
     * Sends an email with an optional file attachment using Mailjet API.
     * If attachmentPath is null or empty, it sends a simple email without
     * attachment.
     */
    public void sendEmailWithAttachment(String toEmail, String subject, String htmlContent, String attachmentPath)
            throws IOException {
        if (!globalSettingsService.isEmailAllowed()) {
            logger.info("Email sending is DISABLED (Master). Skipping email to: {}", toEmail);
            return;
        }

        try {
            MailjetClient client = getMailjetClient();

            // Build the message JSON
            JSONObject message = new JSONObject();
            message.put("From", new JSONObject()
                    .put("Email", fromEmail)
                    .put("Name", fromName));
            message.put("To", new JSONArray()
                    .put(new JSONObject()
                            .put("Email", toEmail)));
            message.put("Subject", subject);
            message.put("HTMLPart", htmlContent);

            // Add attachment if provided
            if (attachmentPath != null && !attachmentPath.isEmpty()) {
                try {
                    Path path = Paths.get(attachmentPath);
                    byte[] fileBytes = Files.readAllBytes(path);
                    String base64Content = Base64.getEncoder().encodeToString(fileBytes);
                    String contentType = Files.probeContentType(path);
                    if (contentType == null) {
                        contentType = "application/octet-stream";
                    }

                    JSONArray attachments = new JSONArray();
                    attachments.put(new JSONObject()
                            .put("ContentType", contentType)
                            .put("Filename", path.getFileName().toString())
                            .put("Base64Content", base64Content));
                    message.put("Attachments", attachments);

                    logger.info("Successfully attached file: {}", path.getFileName().toString());
                } catch (IOException e) {
                    logger.error("Could not read or attach file: {}. Email will be sent without it.", attachmentPath,
                            e);
                }
            }

            // Build the request
            MailjetRequest request = new MailjetRequest(Emailv31.resource)
                    .property(Emailv31.MESSAGES, new JSONArray().put(message));

            // Send the email
            MailjetResponse response = client.post(request);

            if (response.getStatus() >= 200 && response.getStatus() < 300) {
                logger.info("Email successfully sent to {}. Status Code: {}", toEmail, response.getStatus());
            } else {
                throw new IOException("Failed to send email via Mailjet. Status: " + response.getStatus()
                        + " Body: " + response.getData());
            }
        } catch (Exception ex) {
            logger.error("Exception while sending Mailjet email to {}: {}", toEmail, ex.getMessage());
            throw new IOException("Mailjet error: " + ex.getMessage());
        }
    }
}
