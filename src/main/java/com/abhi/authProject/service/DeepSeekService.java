package com.abhi.authProject.service;

import com.abhi.authProject.dto.ResumeAnalysisResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DeepSeekService {

    private static final Logger logger = LoggerFactory.getLogger(DeepSeekService.class);

    @Value("${deepseek.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String API_URL = "https://api.deepseek.com/chat/completions";

    public ResumeAnalysisResponse analyzeResume(String resumeText) {
        try {
            if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_API_KEY_HERE")) {
                logger.warn("DeepSeek API Key is missing. Using Mock Analysis.");
                return getMockAnalysis(
                        "Resume parsing successful, but AI API Key is missing. This is a simulation result.");
            }

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("Content-Type", "application/json");

            Map<String, Object> body = new HashMap<>();
            body.put("model", "deepseek-chat");
            body.put("stream", false);

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content",
                    "You are an expert ATS (Applicant Tracking System) AI. Accept the resume text provided by the user. "
                            +
                            "Analyze it strictly and output a JSON object with the following keys: " +
                            "'score' (integer 0-100), 'summary' (string, strict 2 sentences), 'strengths' (list of strings), "
                            +
                            "'weaknesses' (list of strings), 'missingKeywords' (list of technical keywords missing for a generic SDE role), "
                            +
                            "'recommendedRole' (string, e.g., BackendDev, FrontendDev). Do NOT output markdown, ONLY raw JSON."));
            messages.add(Map.of("role", "user", "content", resumeText));

            body.put("messages", messages);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(API_URL, HttpMethod.POST, entity, String.class);

            // Parse DeepSeek Response
            JsonNode root = objectMapper.readTree(response.getBody());
            String content = root.path("choices").get(0).path("message").path("content").asText();

            // Clean markdown code blocks if present
            if (content.startsWith("```json")) {
                content = content.substring(7, content.length() - 3);
            }

            return objectMapper.readValue(content, ResumeAnalysisResponse.class);

        } catch (HttpClientErrorException e) {
            logger.warn("DeepSeek API Error: {} - {}. Switching to Simulation Mode.", e.getStatusCode(),
                    e.getMessage());
            return getMockAnalysis("DeepSeek API Error (" + e.getStatusCode()
                    + "): Insufficient Balance or Invalid Key. Using Simulation Result.");
        } catch (Exception e) {
            logger.error("Unexpected error in AI Analysis", e);
            return getMockAnalysis("System Error during Analysis. Using Simulation Result.");
        }
    }

    private ResumeAnalysisResponse getMockAnalysis(String message) {
        // Fallback or Rule-based logic could go here
        return new ResumeAnalysisResponse(
                75,
                message,
                List.of("Java", "Spring Boot", "Project Experience"),
                List.of("Missing Cloud Skills", "Formatting inconsistency"),
                List.of("Docker", "Kubernetes", "AWS"),
                "Backend Developer");
    }
}
