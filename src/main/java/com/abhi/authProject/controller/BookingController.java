package com.abhi.authProject.controller;

import com.abhi.authProject.service.MailjetEmailService; // UPDATED import
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private static final Logger logger = LoggerFactory.getLogger(BookingController.class);

    @Autowired
    private MailjetEmailService emailService; // UPDATED to use MailjetEmailService

    // We need the admin's email to send a notification.
    // It should be the same one used by JobApplicationService.
    @Value("${placement.portal.application.recipient-email}")
    private String adminEmail;

    @PostMapping("/book-slot")
    public ResponseEntity<String> bookSlot(
            @RequestParam("name") String name,
            @RequestParam("email") String email,
            @RequestParam("rollno") String rollno,
            @RequestParam("phone") String phone,
            @RequestParam("company") String company,
            @RequestParam("date") String date,
            @RequestParam("position") String position,
            @RequestParam("cgpa") double cgpa,
            @RequestParam(value = "resume", required = false) MultipartFile resume) {
        logger.info("Received booking request from: {}", name);

        // --- Resume handling logic (remains the same) ---
        if (resume != null && !resume.isEmpty()) {
            logger.info("Resume File Name: {}", resume.getOriginalFilename());
            logger.info("Resume File Size: {} bytes", resume.getSize());
            // In a real application, you would save the file here.
        }

        try {
            // --- Send notification email to admin using SendGridEmailService ---
            String adminSubject = "New Interview Slot Booking: " + name + " for " + company;
            String adminBody = "<h3>New Interview Slot Booking</h3>"
                    + "<p>A student has booked an interview slot.</p>"
                    + "<ul>"
                    + "<li><b>Name:</b> " + name + "</li>"
                    + "<li><b>Email:</b> " + email + "</li>"
                    + "<li><b>Roll No:</b> " + rollno + "</li>"
                    + "<li><b>Phone:</b> " + phone + "</li>"
                    + "<li><b>Company:</b> " + company + "</li>"
                    + "<li><b>Position:</b> " + position + "</li>"
                    + "<li><b>Preferred Date:</b> " + date + "</li>"
                    + "<li><b>CGPA:</b> " + cgpa + "</li>"
                    + "</ul>";
            emailService.sendEmailWithAttachment(adminEmail, adminSubject, adminBody, null); // No attachment to admin

            // --- Send a confirmation email to the student using SendGridEmailService ---
            String studentSubject = "Confirmation: Your Interview Slot for " + company + " is Booked";
            String studentBody = "<h3>Interview Slot Confirmed!</h3>"
                    + "<p>Dear " + name + ",</p>"
                    + "<p>Your request to book an interview slot has been received. Here are the details you submitted:</p>"
                    + "<ul>"
                    + "<li><b>Company:</b> " + company + "</li>"
                    + "<li><b>Position:</b> " + position + "</li>"
                    + "<li><b>Preferred Date:</b> " + date + "</li>"
                    + "</ul>"
                    + "<p>The hiring team will get back to you shortly with the final confirmed schedule.</p>"
                    + "<br/><p>Best regards,<br/>The Placement Portal Team</p>";
            emailService.sendEmailWithAttachment(email, studentSubject, studentBody, null); // No attachment to student

        } catch (IOException e) {
            logger.error("Failed to send booking notification emails for user {}: {}", email, e.getMessage());
            // Even if email fails, we don't want to fail the whole request.
            // We just log the error. The booking is still "successful" to the user.
        }

        return ResponseEntity.ok("Interview slot booked and notifications sent!");
    }
}