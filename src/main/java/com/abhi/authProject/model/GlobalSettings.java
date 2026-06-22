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
    private boolean masterEmailEnabled = true; // ✅ ENABLED by default

    // Toggle for New Job Job Alerts (to all students)
    @Column(nullable = false)
    private boolean newJobEmailEnabled = true; // ✅ ENABLED by default

    // Toggle for Application Status Updates (Shortlisted, Selected, Rejected)
    @Column(nullable = false)
    private boolean statusUpdateEmailEnabled = true; // ✅ ENABLED by default

    // Toggle for Company/User Account related emails (Welcome, Verify, etc.)
    @Column(nullable = false)
    private boolean accountEmailEnabled = true; // ✅ ENABLED by default

    // Toggle for Previous Year Paper Download Feature
    @Column(nullable = false)
    private boolean paperDownloadEnabled = false; // ❌ DISABLED by default

    // Toggle for Previous Year Paper Screenshot and Key Restriction Feature
    @Column(nullable = false)
    private boolean screenshotRestrictionEnabled = true; // ✅ ENABLED by default

    // Toggle for Study Notes Download Feature
    @Column(nullable = false)
    private boolean notesDownloadEnabled = false; // ❌ DISABLED by default

    // Toggle for Previous Year Paper Stage Access without Login
    @Column(nullable = false)
    private boolean paperWithoutLoginEnabled = false; // ❌ DISABLED by default

    // Toggle for Study Notes Page Access without Login
    @Column(nullable = false)
    private boolean notesWithoutLoginEnabled = false; // ❌ DISABLED by default
}
