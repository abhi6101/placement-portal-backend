package com.abhi.authProject.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Configuration
public class DatabaseFixer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseFixer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        System.out.println("Running DatabaseFixer to update check constraints...");
        try {
            // Drop existing constraint
            jdbcTemplate
                    .execute("ALTER TABLE job_applications DROP CONSTRAINT IF EXISTS job_applications_status_check");
            System.out.println("Dropped old job_applications_status_check constraint.");

            // Add new constraint with SELECTED included
            String sql = "ALTER TABLE job_applications ADD CONSTRAINT job_applications_status_check " +
                    "CHECK (status IN ('PENDING','SHORTLISTED','ACCEPTED','REJECTED','INTERVIEW_SCHEDULED','INTERVIEW_BOOKED','INTERVIEW_COMPLETED','SELECTED','HIRING_DECISION_MADE'))";
            jdbcTemplate.execute(sql);
            System.out.println("Added new job_applications_status_check constraint with SELECTED status.");
        } catch (Exception e) {
            System.err.println("DatabaseFixer warning: " + e.getMessage());
            // Proceed anyway, maybe it's already done or different DB flavor
        }
    }
}
