package com.abhi.authProject.model;

public enum InterviewStatus {
    SCHEDULED, // HR has proposed a time/link
    BOOKED,    // Student has confirmed the slot
    COMPLETED, // Interview has occurred
    CANCELLED  // Interview cancelled
}
