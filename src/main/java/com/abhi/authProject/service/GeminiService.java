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
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
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

    // List of models to try in order
    private static final String[] MODELS = {
            "gemini-1.5-flash",
            "gemini-1.5-flash-latest",
            "gemini-1.5-flash-001",
            "gemini-pro"
    };

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";

    public ResumeAnalysisResponse analyzeResume(String resumeText, String jobDescription) {
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_API_KEY_HERE")) {
            logger.warn("Gemini API Key is missing. Using Mock Analysis.");
            return getMockAnalysis("Resume parsing successful, but Gemini API Key is missing. Simulation Mode.");
        }

        for (String model : MODELS) {
            try {
                return callGeminiModel(model, resumeText, jobDescription);
            } catch (HttpClientErrorException.NotFound e) {
                logger.warn("Model {} not found (404). Trying next model...", model);
                // Continue to next model
            } catch (Exception e) {
                logger.error("Error calling model {}: {}", model, e.getMessage());
                // For other errors (400, 401, 500), strictly fail or fallback to simulation
                if (e instanceof HttpClientErrorException.TooManyRequests) {
                    return getMockAnalysis("Gemini API Rate Limit Exceeded. Using Simulation Result.");
                }
                // Break loop for non-404 errors to avoid spamming
                break;
            }
        }

        // If all models fail
        return getMockAnalysis("Gemini API Error: Could not connect to any available model. Using Simulation Result.");
    }

    private ResumeAnalysisResponse callGeminiModel(String modelName, String resumeText, String jobDescription)
            throws Exception {
        // Use UriComponentsBuilder to prevent double encoding issues
        String baseUrl = BASE_URL + modelName + ":generateContent";

        URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam("key", apiKey)
                .build()
                .toUri();

        // Debug Log: Print URL with Masked Key
        logger.info("Calling Gemini API: {}", uri.toString().replaceAll("key=[^&]+", "key=MASKED"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        String prompt;

        if (jobDescription != null && !jobDescription.trim().isEmpty()) {
            prompt = "You are an expert Technical Recruiter. Compare the following Resume against the provided Job Description. "
                    +
                    "Output a JSON object with keys: " +
                    "'score' (0-100, based on match), 'summary' (brief comparison summary), " +
                    "'strengths' (matching skills), 'weaknesses' (missing requirements), " +
                    "'missingKeywords' (keywords from JD missing in Resume), 'recommendedRole' (Role title from JD). " +
                    "JSON ONLY. No Markdown. \n\nJOB DESCRIPTION:\n" + jobDescription + "\n\nRESUME:\n" + resumeText;
        } else {
            prompt = "You are an expert HR AI. Analyze the following resume strictly and output a JSON object with keys: "
                    +
                    "'score' (0-100), 'summary' (2 sentences), 'strengths' (list), 'weaknesses' (list), " +
                    "'missingKeywords' (list of missing tech skills for SDE), 'recommendedRole' (string). " +
                    "Do NOT output markdown. JSON ONLY. Resume Text: " + resumeText;
        }

        List<Map<String, Object>> contents = new ArrayList<>();
        Map<String, Object> contentPart = new HashMap<>();
        contentPart.put("parts", List.of(Map.of("text", prompt)));
        contents.add(contentPart);
        body.put("contents", contents);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        // Pass URI object instead of String
        ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, entity, String.class);

        JsonNode root = objectMapper.readTree(response.getBody());
        String text = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();

        if (text.contains("```json")) {
            text = text.replace("```json", "").replace("```", "").trim();
        } else if (text.contains("```")) {
            text = text.replace("```", "").trim();
        }

        return objectMapper.readValue(text, ResumeAnalysisResponse.class);
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
