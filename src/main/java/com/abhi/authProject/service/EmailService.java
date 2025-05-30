package com.abhi.authProject.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    /**
     * Generic method to send an email.
     * @param to The recipient's email address.
     * @param subject The subject of the email.
     * @param body The body content of the email.
     */
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
            // In a production app, you'd want to log this error more robustly
            // throw new RuntimeException("Failed to send email", e);
        }
    }

    // Your existing methods remain the same:
    public void sendBookingNotification(String studentName, String studentEmail,
                                        String rollNumber, String phoneNumber,
                                        String company, String preferredDate,
                                        String position, double cgpa) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail);
            message.setTo(senderEmail);
            message.setSubject("New Interview Slot Booking: " + company);

            String emailContent = "A new interview slot has been booked!\n\n" +
                                    "Student Details:\n" +
                                    "Name: " + studentName + "\n" +
                                    "Email: " + studentEmail + "\n" +
                                    "Roll Number: " + rollNumber + "\n" +
                                    "Phone Number: " + phoneNumber + "\n\n" +
                                    "Booking Details:\n" +
                                    "Company: " + company + "\n" +
                                    "Preferred Date: " + preferredDate + "\n" +
                                    "Position: " + position + "\n" +
                                    "CGPA: " + cgpa + "\n\n" +
                                    "Please review and confirm the booking.";

            message.setText(emailContent);
            mailSender.send(message);
            System.out.println("Email sent successfully to admin: " + senderEmail);

        } catch (MailException e) {
            System.err.println("Error sending email to admin: " + e.getMessage());
        }
    }

    public void sendStudentConfirmationEmail(String studentName, String studentEmail,
                                             String company, String preferredDate,
                                             String position) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail);
            message.setTo(studentEmail);
            message.setSubject("Interview Slot Confirmation: " + company);

            String emailContent = "Dear " + studentName + ",\n\n" +
                                    "Thank you for booking an interview slot for " + company + ".\n" +
                                    "Your requested slot for the " + position + " position on " + preferredDate + " has been noted.\n\n" +
                                    "Further details regarding your interview (exact time, virtual link, etc.) will be shared shortly.\n\n" +
                                    "Best regards,\n" +
                                    " Placement Cell";

            message.setText(emailContent);
            mailSender.send(message);
            System.out.println("Confirmation email sent successfully to student: " + studentEmail);

        } catch (MailException e) {
            System.err.println("Error sending confirmation email to student: " + e.getMessage());
        }
    }
}