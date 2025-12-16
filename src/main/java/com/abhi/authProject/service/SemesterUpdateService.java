package com.abhi.authProject.service;

import com.abhi.authProject.model.Users;
import com.abhi.authProject.repo.UserRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class SemesterUpdateService {

    private static final Logger logger = LoggerFactory.getLogger(SemesterUpdateService.class);

    @Autowired
    private UserRepo userRepo;

    private boolean hasUpdated = false; // Flag to ensure update happens only once

    /**
     * Scheduled task that runs daily at 12:00 AM
     * Checks if date is after 2026-01-01 and updates all IMCA 7th sem students to
     * 8th sem
     */
    @Scheduled(cron = "0 0 0 * * *") // Runs at midnight every day
    public void updateSemesterAfterDate() {
        LocalDate today = LocalDate.now();
        LocalDate targetDate = LocalDate.of(2026, 1, 1);

        // Check if today is on or after January 1, 2026 and update hasn't been done yet
        if (!hasUpdated && (today.isEqual(targetDate) || today.isAfter(targetDate))) {
            logger.info("Date is after 2026-01-01. Updating IMCA 7th semester students to 8th semester...");

            try {
                // Find all students with IMCA branch and semester 7
                List<Users> studentsToUpdate = userRepo.findByBranchAndSemester("IMCA", 7);

                int updateCount = 0;
                for (Users student : studentsToUpdate) {
                    student.setSemester(8);
                    userRepo.save(student);
                    updateCount++;
                }

                hasUpdated = true; // Mark as updated
                logger.info("Successfully updated {} students from IMCA 7th to 8th semester", updateCount);

            } catch (Exception e) {
                logger.error("Error updating semester: {}", e.getMessage());
            }
        }
    }

    /**
     * Manual trigger method (can be called via admin endpoint if needed)
     */
    public String manualSemesterUpdate() {
        List<Users> studentsToUpdate = userRepo.findByBranchAndSemester("IMCA", 7);

        int updateCount = 0;
        for (Users student : studentsToUpdate) {
            student.setSemester(8);
            userRepo.save(student);
            updateCount++;
        }

        hasUpdated = true;
        return "Updated " + updateCount + " students from IMCA 7th to 8th semester";
    }
}
