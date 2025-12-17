package com.abhi.authProject.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configures Cross-Origin Resource Sharing (CORS) for the application.
 * This allows the frontend client, hosted on a different domain, to communicate
 * with this backend server.
 */
@Configuration
public class WebConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**") // Apply CORS to all endpoints, including /jobs
                        .allowedOrigins(
                                "http://localhost:5500", // For local development (e.g., VS Code Live Server)
                                "http://127.0.0.1:5500", // Alternative for local development
                                "https://hack-2-hired.onrender.com" // ** IMPORTANT: Your public frontend URL **
                )
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*") // Allows all headers
                        .allowCredentials(true) // Allows cookies and authorization headers
                        .maxAge(3600); // Caches pre-flight response for 1 hour
            }
        };
    }
}