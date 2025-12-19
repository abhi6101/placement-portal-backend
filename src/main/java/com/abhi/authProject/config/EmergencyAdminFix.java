package com.abhi.authProject.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;

@Configuration
public class EmergencyAdminFix {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Bean
    CommandLineRunner fixDatabaseSchema() {
            System.out.println("🚨 EMERGENCY DB PATCHER: Checking Schema Health...");

            // 1. Fix Users Table (Add 'batch' if missing)
            try {
                // Set a short lock timeout so we don't hang the app if we can't get the lock
                jdbcTemplate.execute("SET lock_timeout = '5s'");

                // Check if column exists first to avoid unnecessary ALTER TABLE locks
                Integer count = jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM information_schema.columns WHERE table_name='users' AND column_name='batch'",
                        Integer.class);

                if (count == null || count == 0) {
                    System.out.println("🚀 'batch' column missing in 'users', attempting to add...");
                    jdbcTemplate.execute("ALTER TABLE users ADD COLUMN batch VARCHAR(255)");
                    System.out.println("✅ Successfully added 'batch' column to 'users'");
                } else {
                    System.out.println("✅ 'batch' column already exists in 'users'");
                }
            } catch (Exception e) {
                System.err.println("⚠️ Error patching users table: " + e.getMessage());
            }

            // 2. Create id_card_image table manually
            try {
                jdbcTemplate.execute(
                        "CREATE TABLE IF NOT EXISTS id_card_image (id BIGSERIAL PRIMARY KEY, name VARCHAR(255), type VARCHAR(255), data BYTEA)");
                System.out.println("✅ Checked/Created 'id_card_image' table");
            } catch (Exception e) {
                System.err.println("⚠️ Error creating id_card_image table: " + e.getMessage());
            }

            // 3. Fix Student Profiles (Add new columns)
            try {
                // Same surgical check for student_profiles
                String[] cols = { "college_name", "aadhar_card_url", "admit_card_url", "id_card_image_id",
                        "aadhar_image_id", "admit_card_image_id" };
                for (String col : cols) {
                    Integer count = jdbcTemplate.queryForObject(
                            "SELECT count(*) FROM information_schema.columns WHERE table_name='student_profiles' AND column_name=?",
                            Integer.class, col);
                    if (count == null || count == 0) {
                        String type = col.endsWith("_id") ? "BIGINT" : "VARCHAR(255)";
                        jdbcTemplate.execute("ALTER TABLE student_profiles ADD COLUMN " + col + " " + type);
                        System.out.println("✅ Added missing column '" + col + "' to 'student_profiles'");
                    }
                }
            } catch (Exception e) {
                System.err.println("⚠️ Error patching student_profiles table: " + e.getMessage());
            }

            System.out.println("✅ Schema Patching Complete. Hibernate should validate now.");
        };
    }
}
