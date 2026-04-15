package com.abhi.authProject.controller;

import com.abhi.authProject.model.Interview;
import com.abhi.authProject.model.InterviewBookingRequest;
import com.abhi.authProject.model.InterviewScheduleRequest;
import com.abhi.authProject.service.InterviewService;
import jakarta.validation.Valid;
import org.slf4j.Logger; // <--- ADD THIS IMPORT
import org.slf4j.LoggerFactory; // <--- ADD THIS IMPORT
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class InterviewController {

    // ADD THIS LINE TO CREATE THE LOGGER
    private static final Logger logger = LoggerFactory.getLogger(InterviewController.class);

    private final InterviewService interviewService;

    public InterviewController(InterviewService interviewService) {
        this.interviewService = interviewService;
    }

    @PostMapping("/admin/interviews")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Interview> scheduleInterview(@Valid @RequestBody InterviewScheduleRequest request) {
        try {
            Interview interview = interviewService.scheduleInterview(request);
            return new ResponseEntity<>(interview, HttpStatus.CREATED);
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            logger.error("Error scheduling interview or sending email: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to schedule interview and send email.");
        }
    }

    @GetMapping("/user/interviews")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<Interview>> getMyInterviews(@AuthenticationPrincipal UserDetails userDetails) {
        String applicantEmail = userDetails.getUsername();
        List<Interview> interviews = interviewService.getInterviewsForApplicant(applicantEmail);
        return ResponseEntity.ok(interviews);
    }

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
        } catch (Exception e) {
            logger.error("Error booking interview or sending email: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to book interview and send email.");
        }
    }

    @GetMapping("/admin/interviews/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Interview> getInterviewById(@PathVariable Long id) {
        return interviewService.getInterviewById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Interview not found"));
    }
}