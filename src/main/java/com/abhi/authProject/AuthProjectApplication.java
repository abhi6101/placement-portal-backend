// fully backend/src/main/java/com/abhi/authProject/AuthProjectApplication.java

package com.abhi.authProject;

import io.github.cdimascio.dotenv.Dotenv; // Make sure this import is present
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class AuthProjectApplication {

    public static void main(String[] args) {
        // --- START OF .env LOADING CODE ---
        // This block should be at the very beginning of your main method
        try {
            Dotenv dotenv = Dotenv.load(); // This will look for .env in the current working directory
            dotenv.entries().forEach(entry ->
                System.setProperty(entry.getKey(), entry.getValue())
            );
        } catch (Exception e) {
            // Log or handle the exception if .env is not found (e.g., in production where env vars are set externally)
            System.out.println(".env file not loaded (might be missing or not applicable in this environment): " + e.getMessage());
        }
        // --- END OF .env LOADING CODE ---

        SpringApplication.run(AuthProjectApplication.class, args);
    }
}