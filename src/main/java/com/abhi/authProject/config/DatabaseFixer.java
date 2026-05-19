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

        System.out.println("🔨 Ensuring paper_view_logs table exists...");
        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS paper_view_logs (" +
                    "id BIGSERIAL PRIMARY KEY, " +
                    "username VARCHAR(255) NOT NULL, " +
                    "student_name VARCHAR(255), " +
                    "computer_code VARCHAR(255), " +
                    "paper_id BIGINT NOT NULL, " +
                    "paper_title VARCHAR(255) NOT NULL, " +
                    "subject VARCHAR(255), " +
                    "branch VARCHAR(255), " +
                    "semester INTEGER NOT NULL, " +
                    "year INTEGER NOT NULL, " +
                    "viewed_at TIMESTAMP NOT NULL" +
                    ")");
            System.out.println("✅ Table paper_view_logs check completed successfully.");
        } catch (Exception e) {
            System.err.println("⚠️ DatabaseFixer warning creating paper_view_logs: " + e.getMessage());
        }

        System.out.println("🔨 Ensuring security columns exist in users table...");
        try {
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS security_strikes INTEGER DEFAULT 0");
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS locked_until TIMESTAMP");
            System.out.println("✅ Security columns checked/added successfully.");
        } catch (Exception e) {
            System.err.println("⚠️ DatabaseFixer warning altering users table: " + e.getMessage());
        }
    }
}
