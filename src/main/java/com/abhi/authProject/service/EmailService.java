package com.abhi.authProject.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async; // Add this import

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${SENDER_FROM_EMAIL}")
    private String senderEmail;

    /**
     * Generic method to send an email asynchronously.
     * @param to The recipient's email address.
     * @param subject The subject of the email.
     * @param body The body content of the email.
     */
    @Async // CORRECTED: Add this annotation to make the method run in a background thread
    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            System.out.println("Email sent successfully to: " + to + " with subject: " + subject);
        } catch (MailException e) {
            System.err.println("Error sending email to " + to + ": " + e.getMessage());
        }
    }

    // Your existing methods remain the same...
    // (sendBookingNotification and sendStudentConfirmationEmail)
    // They will now call the async sendEmail method if you refactor them,
    // or you can add @Async to them as well if they are called directly.

    public void sendBookingNotification(String studentName, String studentEmail,
                                        String rollNumber, String phoneNumber,
                                        String company, String preferredDate,
                                        String position, double cgpa) {
        // This method's implementation can remain the same
        // ...
    }

    public void sendStudentConfirmationEmail(String studentName, String studentEmail,
                                             String company, String preferredDate,
                                             String position) {
        // This method's implementation can remain the same
        // ...
    }
}