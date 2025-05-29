package com.abhi.authProject.model;


public enum ApplicationStatus {
    PENDING,        // Initial status when student applies
    ACCEPTED,       // HR has reviewed and accepted the application
    REJECTED,       // HR has reviewed and rejected the application
    INTERVIEW_SCHEDULED, // An interview has been scheduled by HR
    INTERVIEW_BOOKED,    // Student has confirmed/booked the interview slot
    INTERVIEW_COMPLETED, // Interview has taken place
    HIRING_DECISION_MADE // Final decision made
}
