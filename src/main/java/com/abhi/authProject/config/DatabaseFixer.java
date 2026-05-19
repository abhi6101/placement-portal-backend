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
        System.out.println("🚀 Running DatabaseFixer to clear old test/demo applications...");
        try {
            int deletedJobApps = jdbcTemplate.update("DELETE FROM job_applications");
            int deletedApps = jdbcTemplate.update("DELETE FROM applications");
            System.out.println("✅ Purged " + deletedJobApps + " from job_applications and " + deletedApps + " from applications.");
        } catch (Exception e) {
            System.err.println("⚠️ DatabaseFixer warning: " + e.getMessage());
        }
    }
}
