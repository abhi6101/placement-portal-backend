package com.abhi.authProject.model;


import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class InterviewBookingRequest {
    @NotNull
    private LocalDateTime studentBookedDateTime; // Student confirms this time (could be same as scheduledDateTime)

    
    // Getters and Setters
    public LocalDateTime getStudentBookedDateTime() { return studentBookedDateTime; }
    public void setStudentBookedDateTime(LocalDateTime studentBookedDateTime) { this.studentBookedDateTime = studentBookedDateTime; }
}