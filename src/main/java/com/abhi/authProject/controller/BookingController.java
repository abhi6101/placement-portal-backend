package com.abhi.authProject.controller;

// src/main/java/com/yourcompany/placementportal/controller/BookingController.java

import com.abhi.authProject.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    @Autowired
    private EmailService emailService;

    // You might want to define a DTO (Data Transfer Object) for cleaner code
    // For simplicity, using Map<String, String> here for form data
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
            @RequestParam(value = "resume", required = false) MultipartFile resume // Resume is optional for now
    ) {
        // In a real application, you'd save this booking data to a database first
        System.out.println("Received booking request:");
        System.out.println("Name: " + name);
        System.out.println("Email: " + email);
        System.out.println("Roll No: " + rollno);
        System.out.println("Phone: " + phone);
        System.out.println("Company: " + company);
        System.out.println("Date: " + date);
        System.out.println("Position: " + position);
        System.out.println("CGPA: " + cgpa);

        // You'll need to handle resume file upload and storage securely.
        // For example, save it to a file system or cloud storage.
        if (resume != null && !resume.isEmpty()) {
            try {
                // Example: print file details (DO NOT save directly without validation in production)
                System.out.println("Resume File Name: " + resume.getOriginalFilename());
                System.out.println("Resume File Size: " + resume.getSize() + " bytes");
                // You would typically save this file to a designated location
                // Path filePath = Paths.get("path/to/save/resumes/" + resume.getOriginalFilename());
                // Files.copy(resume.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                System.err.println("Failed to upload resume: " + e.getMessage());
                // Handle upload error
                return ResponseEntity.status(500).body("Error processing resume upload.");
            }
        }

        // Send notification email to admin
        emailService.sendBookingNotification(name, email, rollno, phone, company, date, position, cgpa);

        // Optionally, send a confirmation email to the student
        emailService.sendStudentConfirmationEmail(name, email, company, date, position);

        // Return a success response
        return ResponseEntity.ok("Interview slot booked and admin notified!");
    }
}
