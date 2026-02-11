package com.abhi.authProject.controller;

import com.abhi.authProject.model.GlobalSettings;
import com.abhi.authProject.service.GlobalSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class EmailTestController {

    @Autowired
    private GlobalSettingsService settingsService;

    /**
     * TEMPORARY ENDPOINT: Enable all email settings
     * Call this once to enable emails, then you can delete this controller
     */
    @PostMapping("/enable-emails")
    public ResponseEntity<?> enableEmails() {
        GlobalSettings settings = settingsService.getSettings();
        settings.setMasterEmailEnabled(true);
        settings.setNewJobEmailEnabled(true);
        settings.setStatusUpdateEmailEnabled(true);
        settings.setAccountEmailEnabled(true);

        GlobalSettings updated = settingsService.updateSettings(settings);

        return ResponseEntity.ok(Map.of(
                "message", "✅ All email settings enabled!",
                "masterEmailEnabled", updated.isMasterEmailEnabled(),
                "newJobEmailEnabled", updated.isNewJobEmailEnabled(),
                "statusUpdateEmailEnabled", updated.isStatusUpdateEmailEnabled(),
                "accountEmailEnabled", updated.isAccountEmailEnabled()));
    }

    /**
     * Check current email settings
     */
    @GetMapping("/email-settings")
    public ResponseEntity<?> getEmailSettings() {
        GlobalSettings settings = settingsService.getSettings();

        return ResponseEntity.ok(Map.of(
                "masterEmailEnabled", settings.isMasterEmailEnabled(),
                "newJobEmailEnabled", settings.isNewJobEmailEnabled(),
                "statusUpdateEmailEnabled", settings.isStatusUpdateEmailEnabled(),
                "accountEmailEnabled", settings.isAccountEmailEnabled()));
    }
}
