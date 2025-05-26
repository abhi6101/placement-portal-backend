package com.abhi.authProject.config;

// fully backend/src/main/java/com/example/yourapp/YourApplication.java (main class)


import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class YourApplication {

    public static void main(String[] args) {
        // Load .env file for local development
        Dotenv dotenv = Dotenv.load();
        dotenv.entries().forEach(entry ->
            System.setProperty(entry.getKey(), entry.getValue())
        );
        SpringApplication.run(YourApplication.class, args);
    }
}