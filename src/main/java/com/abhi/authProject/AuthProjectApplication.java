package com.abhi.authProject;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync; // Add this import

@SpringBootApplication
@EnableAsync // CORRECTED: Add this annotation to enable async operations
public class AuthProjectApplication {

    public static void main(String[] args) {
        try {
            Dotenv dotenv = Dotenv.load();
            dotenv.entries().forEach(entry ->
                System.setProperty(entry.getKey(), entry.getValue())
            );
        } catch (Exception e) {
            System.out.println(".env file not loaded (might be missing or not applicable in this environment): " + e.getMessage());
        }

        SpringApplication.run(AuthProjectApplication.class, args);
    }
}