package com.abhi.authProject.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

@Service
public class EmailService1 {

    
    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String adminEmail;


    // ... (Your existing sendBookingNotification and sendStudentConfirmationEmail methods) ...

    public void sendJobApplicationNotification(String jobTitle, String companyName,
                                             String applicantName, String applicantEmail,
                                             String applicantRollNo, String applicantPhone) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(adminEmail);
            message.setTo(adminEmail); // Send to admin
            message.setSubject("New Job Application: " + jobTitle + " at " + companyName);

            String emailContent = "A new application has been submitted for a job!\n\n" +
                                  "Job Details:\n" +
                                  "Title: " + jobTitle + "\n" +
                                  "Company: " + companyName + "\n\n" +
                                  "Applicant Details:\n" +
                                  "Name: " + applicantName + "\n" +
                                  "Email: " + applicantEmail + "\n" +
                                  "Roll No: " + applicantRollNo + "\n" +
                                  "Phone: " + applicantPhone + "\n\n" +
                                  "Please review the application.";

            message.setText(emailContent);
            mailSender.send(message);
            System.out.println("Job application notification email sent successfully to admin: " + adminEmail);

        } catch (MailException e) {
            System.err.println("Error sending job application notification email to admin: " + e.getMessage());
            // Log the exception properly in a real application
        }
    }
}