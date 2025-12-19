package com.abhi.authProject.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class EmergencyAdminFix {

    /*
     * Manual SQL required in Supabase:
     * ALTER TABLE users ADD COLUMN IF NOT EXISTS batch VARCHAR(255);
     * ALTER TABLE student_profiles ADD COLUMN IF NOT EXISTS college_name
     * VARCHAR(255),
     * ADD COLUMN IF NOT EXISTS aadhar_card_url VARCHAR(255),
     * ADD COLUMN IF NOT EXISTS admit_card_url VARCHAR(255),
     * ADD COLUMN IF NOT EXISTS id_card_image_id BIGINT,
     * ADD COLUMN IF NOT EXISTS aadhar_image_id BIGINT,
     * ADD COLUMN IF NOT EXISTS admit_card_image_id BIGINT;
     */
}
