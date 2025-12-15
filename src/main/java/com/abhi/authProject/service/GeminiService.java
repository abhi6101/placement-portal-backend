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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiService.class);

    @Value("${gemini.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";

    public ResumeAnalysisResponse analyzeResume(String resumeText) {
        try {
            if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_API_KEY_HERE")) {
                logger.warn("Gemini API Key is missing. Using Mock Analysis.");
                return getMockAnalysis("Resume parsing successful, but Gemini API Key is missing. Simulation Mode.");
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Construct Gemini Request Body
            // { "contents": [{ "parts": [{ "text": "..." }] }] }
            Map<String, Object> body = new HashMap<>();

            String prompt = "You are an expert HR AI. Analyze the following resume text strictly and output a JSON object with keys: "
                    +
                    "'score' (0-100), 'summary' (2 sentences), 'strengths' (list), 'weaknesses' (list), " +
                    "'missingKeywords' (list of missing tech skills for SDE), 'recommendedRole' (string). " +
                    "Do NOT output markdown. JSON ONLY. Resume Text: " + resumeText;

            List<Map<String, Object>> contents = new ArrayList<>();
            Map<String, Object> contentPart = new HashMap<>();
            contentPart.put("parts", List.of(Map.of("text", prompt)));
            contents.add(contentPart);

            body.put("contents", contents);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            // Append Key to URL
            String url = GEMINI_URL + "?key=" + apiKey;

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            // Parse Gemini Response
            JsonNode root = objectMapper.readTree(response.getBody());
            String text = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();

            // Clean Markdown ```json ... ```
            if (text.contains("```json")) {
                text = text.replace("```json", "").replace("```", "").trim();
            } else if (text.contains("```")) {
                text = text.replace("```", "").trim();
            }

            return objectMapper.readValue(text, ResumeAnalysisResponse.class);

        } catch (HttpClientErrorException e) {
            logger.warn("Gemini API Error: {} - {}. Simulation Mode.", e.getStatusCode(), e.getMessage());
            return getMockAnalysis("Gemini API Error (" + e.getStatusCode() + "). Using Simulation Result.");
        } catch (Exception e) {
            logger.error("Unexpected error in AI Analysis", e);
            return getMockAnalysis("System Error during Analysis. Using Simulation Result.");
        }
    }

    private ResumeAnalysisResponse getMockAnalysis(String message) {
        return new ResumeAnalysisResponse(
                82,
                message,
                List.of("Java", "Spring Boot", "Critical Thinking"),
                List.of("Missing Cloud Certifications", "Resume Formatting"),
                List.of("GCP", "Kubernetes", "Microservices"),
                "Full Stack Developer");
    }
}
