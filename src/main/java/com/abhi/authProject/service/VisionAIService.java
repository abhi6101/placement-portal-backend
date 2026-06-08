package com.abhi.authProject.service;

import com.abhi.authProject.model.VisionAIResponseDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class VisionAIService {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public VisionAIResponseDto analyzeImages(List<MultipartFile> images) {
        VisionAIResponseDto responseDto = new VisionAIResponseDto();

        if (images == null || images.isEmpty()) {
            responseDto.setQualityGood(false);
            responseDto.setMessage("No images provided.");
            return responseDto;
        }

        try {
            // Process only the first image to save tokens
            MultipartFile firstImage = images.get(0);
            String base64Image = Base64.getEncoder().encodeToString(firstImage.getBytes());
            String mimeType = firstImage.getContentType();
            if (mimeType == null || !mimeType.startsWith("image/")) {
                mimeType = "image/jpeg";
            }

            String prompt = "Analyze this university exam paper and extract: subject, semester, branch, year. " +
                    "Also verify if it's a printed official paper and not handwritten notes. " +
                    "Respond in STRICT JSON format like this: " +
                    "{\"qualityGood\": true, \"subject\": \"Computer Networks\", \"semester\": 5, \"branch\": \"Computer Science\", \"year\": \"2024\", \"message\": \"Success\"}. " +
                    "If it is handwritten or illegible or not a question paper, return: " +
                    "{\"qualityGood\": false, \"message\": \"Upload Failed: The image appears to be handwritten or blurry. We only accept official, printed university exam papers.\"}";

            Map<String, Object> textPart = new HashMap<>();
            textPart.put("text", prompt);

            Map<String, Object> inlineData = new HashMap<>();
            inlineData.put("mimeType", mimeType);
            inlineData.put("data", base64Image);

            Map<String, Object> imagePart = new HashMap<>();
            imagePart.put("inlineData", inlineData);

            Map<String, Object> contentPart = new HashMap<>();
            contentPart.put("parts", new Object[]{textPart, imagePart});

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", new Object[]{contentPart});

            // Optional: force JSON response
            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("responseMimeType", "application/json");
            requestBody.put("generationConfig", generationConfig);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + geminiApiKey;

            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode rootNode = objectMapper.readTree(response.getBody());
                JsonNode candidates = rootNode.path("candidates");
                if (candidates.isArray() && candidates.size() > 0) {
                    JsonNode parts = candidates.get(0).path("content").path("parts");
                    if (parts.isArray() && parts.size() > 0) {
                        String jsonText = parts.get(0).path("text").asText();
                        
                        // Strip markdown formatting if Gemini still included it
                        jsonText = jsonText.replaceAll("```json", "").replaceAll("```", "").trim();

                        JsonNode resultNode = objectMapper.readTree(jsonText);
                        
                        boolean isGood = resultNode.path("qualityGood").asBoolean(false);
                        responseDto.setQualityGood(isGood);
                        
                        if (isGood) {
                            responseDto.setSubject(resultNode.path("subject").asText(""));
                            responseDto.setSemester(resultNode.path("semester").asInt(0));
                            responseDto.setBranch(resultNode.path("branch").asText(""));
                            responseDto.setYear(resultNode.path("year").asText(""));
                            responseDto.setMessage("Success");
                        } else {
                            responseDto.setMessage(resultNode.path("message").asText("Image quality check failed."));
                        }
                        return responseDto;
                    }
                }
            }

            responseDto.setQualityGood(false);
            responseDto.setMessage("Failed to process the image with AI.");

        } catch (Exception e) {
            e.printStackTrace();
            responseDto.setQualityGood(false);
            responseDto.setMessage("An error occurred while analyzing the image: " + e.getMessage());
        }

        return responseDto;
    }
}
