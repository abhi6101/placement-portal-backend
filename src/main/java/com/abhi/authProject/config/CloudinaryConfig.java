package com.abhi.authProject.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudinaryConfig {

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    @Bean
    public Cloudinary cloudinary() {
        // Trim whitespace from credentials to avoid URL formatting issues
        String trimmedCloudName = cloudName != null ? cloudName.trim() : "";
        String trimmedApiKey = apiKey != null ? apiKey.trim() : "";
        String trimmedApiSecret = apiSecret != null ? apiSecret.trim() : "";

        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", trimmedCloudName,
                "api_key", trimmedApiKey,
                "api_secret", trimmedApiSecret,
                "secure", true));
    }
}
