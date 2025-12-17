package com.abhi.authProject.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "global_settings")
@Data
public class GlobalSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Master switch: If false, NO emails are sent regardless of other settings
    @Column(nullable = false)
    private boolean masterEmailEnabled = false;

    // Toggle for New Job Job Alerts (to all students)
    @Column(nullable = false)
    private boolean newJobEmailEnabled = false;

    // Toggle for Application Status Updates (Shortlisted, Selected, Rejected)
    @Column(nullable = false)
    private boolean statusUpdateEmailEnabled = false;

    // Toggle for Company/User Account related emails (Welcome, Verify, etc.)
    @Column(nullable = false)
    private boolean accountEmailEnabled = false;
}
