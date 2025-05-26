package com.abhi.authProject.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value; // <--- This is the missing import

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    // This will now fetch the value from spring.mail.username in application.properties
    @Value("${spring.mail.username}")
    private String adminEmail;



    public void sendBookingNotification(String studentName, String studentEmail,
                                        String rollNumber, String phoneNumber,
                                        String company, String preferredDate,
                                        String position, double cgpa) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(adminEmail); // Sender's email (your admin email)
            message.setTo(adminEmail);   // Admin's email (where you want to receive the notification)
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
            System.out.println("Email sent successfully to admin: " + adminEmail);

        } catch (MailException e) {
            System.err.println("Error sending email to admin: " + e.getMessage());
            // Log the exception properly in a real application
        }
    }

    public void sendStudentConfirmationEmail(String studentName, String studentEmail,
                                                String company, String preferredDate,
                                                String position) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(adminEmail); // Sender's email (your admin email)
            message.setTo(studentEmail); // Student's email
            message.setSubject("Interview Slot Confirmation: " + company);

            String emailContent = "Dear " + studentName + ",\n\n" +
                                  "Thank you for booking an interview slot for " + company + ".\n" +
                                  "Your requested slot for the " + position + " position on " + preferredDate + " has been noted.\n\n" +
                                  "Further details regarding your interview (exact time, virtual link, etc.) will be shared shortly.\n\n" +
                                  "Best regards,\n" +
                                  "College Placement Portal";

            message.setText(emailContent);
            mailSender.send(message);
            System.out.println("Confirmation email sent successfully to student: " + studentEmail);

        } catch (MailException e) {
            System.err.println("Error sending confirmation email to student: " + e.getMessage());
            // Log the exception properly
        }
    }
}