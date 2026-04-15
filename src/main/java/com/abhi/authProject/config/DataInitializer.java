package com.abhi.authProject.config;

import com.abhi.authProject.model.DepartmentBranch;
import com.abhi.authProject.repo.DepartmentBranchRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Data initialization and fixes that run automatically on application startup.
 * This ensures data consistency across all deployments.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final DepartmentBranchRepo departmentBranchRepo;

    @Override
    public void run(String... args) throws Exception {
        log.info("🔧 Running data initialization and fixes...");

        fixImcaDuration();

        log.info("✅ Data initialization completed successfully!");
    }

    /**
     * Fix IMCA course duration from 8 semesters (4 years) to 10 semesters (5
     * years).
     * IMCA (Integrated Master of Computer Applications) is a 5-year integrated
     * program.
     */
    private void fixImcaDuration() {
        try {
            DepartmentBranch imca = departmentBranchRepo.findByBranchCode("IMCA").orElse(null);

            if (imca != null) {
                if (imca.getMaxSemesters() != 10) {
                    log.info("📚 Fixing IMCA duration: {} semesters → 10 semesters", imca.getMaxSemesters());
                    imca.setMaxSemesters(10);
                    departmentBranchRepo.save(imca);
                    log.info("✅ IMCA duration fixed successfully! Now shows 5 years (10 semesters)");
                } else {
                    log.info("✓ IMCA duration already correct (10 semesters)");
                }
            } else {
                log.warn("⚠️ IMCA branch not found in database. Skipping fix.");
            }
        } catch (Exception e) {
            log.error("❌ Error fixing IMCA duration: {}", e.getMessage(), e);
        }
    }
}
