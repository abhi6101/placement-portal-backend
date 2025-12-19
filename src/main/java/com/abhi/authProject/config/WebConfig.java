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

    @org.springframework.beans.factory.annotation.Value("${FRONTEND_URL}")
    private String frontendUrl;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**") // Apply CORS to all endpoints
                        .allowedOrigins(
                                frontendUrl,
                                "https://hack-2-hired.vercel.app",
                                "https://hack-2-hired.vercel.app/")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true)
                        .maxAge(3600);
            }
        };
    }
}