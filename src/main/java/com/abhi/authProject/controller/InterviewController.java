package com.abhi.authProject.controller;

import com.abhi.authProject.model.InterviewBookingRequest;
import com.abhi.authProject.model.InterviewScheduleRequest;
import com.abhi.authProject.model.Interview;
import com.abhi.authProject.service.InterviewService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
// Remove @CrossOrigin here if you manage CORS globally in SecurityConfig
// @CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500"}, allowCredentials = "true")
public class InterviewController {

    private final InterviewService interviewService;

    public InterviewController(InterviewService interviewService) {
        this.interviewService = interviewService;
    }

    // Endpoint for HR/Admin to schedule an interview
    @PostMapping("/admin/interviews")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Interview> scheduleInterview(@Valid @RequestBody InterviewScheduleRequest request) {
        try {
            Interview interview = interviewService.scheduleInterview(request);
            return new ResponseEntity<>(interview, HttpStatus.CREATED);
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (MessagingException e) {
            System.err.println("Error scheduling interview or sending email: " + e.getMessage());
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to schedule interview and send email: " + e.getMessage());
        }
    }

    // Endpoint for Student to view their interviews
    @GetMapping("/user/interviews")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<Interview>> getMyInterviews(@AuthenticationPrincipal UserDetails userDetails) {
        // Assuming userDetails.getUsername() corresponds to the applicant's email
        // You might need to retrieve the applicant's email from your User entity based on userId from JWT
        String applicantEmail = userDetails.getUsername(); // Or retrieve from a custom User entity/service
        List<Interview> interviews = interviewService.getInterviewsForApplicant(applicantEmail);
        return ResponseEntity.ok(interviews);
    }

    // Endpoint for Student to book an interview slot
    @PutMapping("/user/interviews/{interviewId}/book-slot")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Interview> bookInterviewSlot(
            @PathVariable Long interviewId,
            @Valid @RequestBody InterviewBookingRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try {
            Optional<Interview> interview = interviewService.getInterviewById(interviewId);
            if (interview.isEmpty() || !interview.get().getJobApplication().getApplicantEmail().equals(userDetails.getUsername())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to book this interview.");
            }
            Interview bookedInterview = interviewService.bookInterviewSlot(interviewId, request);
            return ResponseEntity.ok(bookedInterview);
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (MessagingException e) {
            System.err.println("Error booking interview or sending email: " + e.getMessage());
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to book interview and send email: " + e.getMessage());
        }
    }

    // HR/Admin can also view specific interviews if needed
    @GetMapping("/admin/interviews/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Interview> getInterviewById(@PathVariable Long id) {
        Optional<Interview> interview = interviewService.getInterviewById(id);
        return interview.map(ResponseEntity::ok)
                          .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Interview not found"));
    }
}
