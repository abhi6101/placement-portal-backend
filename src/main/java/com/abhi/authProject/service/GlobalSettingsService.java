package com.abhi.authProject.service;

import com.abhi.authProject.model.GlobalSettings;
import com.abhi.authProject.repo.GlobalSettingsRepo;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GlobalSettingsService {

    @Autowired
    private GlobalSettingsRepo repo;

    // Cache settings in memory for performance, update on change
    private GlobalSettings cachedSettings;

    @PostConstruct
    public void init() {
        getSettings(); // Initialize cache
    }

    public GlobalSettings getSettings() {
        List<GlobalSettings> all = repo.findAll();
        if (all.isEmpty()) {
            GlobalSettings defaultSettings = new GlobalSettings();
            // Defaults are true
            cachedSettings = repo.save(defaultSettings);
        } else {
            cachedSettings = all.get(0);
        }
        return cachedSettings;
    }

    public GlobalSettings updateSettings(GlobalSettings newSettings) {
        GlobalSettings existing = getSettings(); // usage of 'existing' ensures we update the single row

        existing.setMasterEmailEnabled(newSettings.isMasterEmailEnabled());
        existing.setNewJobEmailEnabled(newSettings.isNewJobEmailEnabled());
        existing.setStatusUpdateEmailEnabled(newSettings.isStatusUpdateEmailEnabled());
        existing.setAccountEmailEnabled(newSettings.isAccountEmailEnabled());

        cachedSettings = repo.save(existing);
        return cachedSettings;
    }

    // Helper methods for EmailService checks
    public boolean isEmailAllowed() {
        getSettings(); // Refresh/Ensure loaded
        return cachedSettings.isMasterEmailEnabled();
    }

    public boolean isNewJobEmailAllowed() {
        getSettings();
        return cachedSettings.isMasterEmailEnabled() && cachedSettings.isNewJobEmailEnabled();
    }

    public boolean isStatusUpdateEmailAllowed() {
        getSettings();
        return cachedSettings.isMasterEmailEnabled() && cachedSettings.isStatusUpdateEmailEnabled();
    }

    public boolean isAccountEmailAllowed() {
        getSettings();
        return cachedSettings.isMasterEmailEnabled() && cachedSettings.isAccountEmailEnabled();
    }
}
