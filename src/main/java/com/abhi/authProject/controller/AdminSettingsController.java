package com.abhi.authProject.controller;

import com.abhi.authProject.model.GlobalSettings;
import com.abhi.authProject.service.GlobalSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/settings")
public class AdminSettingsController {

    @Autowired
    private GlobalSettingsService settingsService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<GlobalSettings> getSettings() {
        return ResponseEntity.ok(settingsService.getSettings());
    }

    @PutMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<GlobalSettings> updateSettings(@RequestBody GlobalSettings newSettings) {
        GlobalSettings updated = settingsService.updateSettings(newSettings);
        return ResponseEntity.ok(updated);
    }
}
