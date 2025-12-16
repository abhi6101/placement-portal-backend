package com.abhi.authProject.config;

import com.abhi.authProject.model.Users;
import com.abhi.authProject.repo.UserRepo;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class EmergencyAdminFix {

    @Bean
    CommandLineRunner fixAdminVerification(UserRepo userRepo) {
        return args -> {
            System.out.println("🚨 EMERGENCY FIX: Checking Admin Verification Status...");

            List<Users> admins = userRepo.findAll().stream()
                    .filter(u -> "SUPER_ADMIN".equals(u.getRole()) || "ADMIN".equals(u.getRole()))
                    .toList();

            for (Users admin : admins) {
                if (!admin.isVerified()) {
                    System.out.println("✅ Fixing verification for admin: " + admin.getUsername());
                    admin.setVerified(true);
                    userRepo.save(admin);
                }
            }
            System.out.println("✅ Admin Verification Check Complete.");
        };
    }
}
