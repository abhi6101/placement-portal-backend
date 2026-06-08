package com.abhi.authProject.service;

import com.abhi.authProject.model.VisionAIResponseDto;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Random;

@Service
public class VisionAIService {

    /**
     * Mocks the Google Gemini Vision API call.
     * In the future, you will integrate the real API here using restTemplate or WebClient.
     */
    public VisionAIResponseDto analyzeImages(List<MultipartFile> images) {
        VisionAIResponseDto response = new VisionAIResponseDto();

        // Basic validation
        if (images == null || images.isEmpty()) {
            response.setQualityGood(false);
            response.setMessage("No images provided.");
            return response;
        }

        // --- MOCK LOGIC ---
        // For demonstration, we simulate that 90% of uploads are good, 10% are rejected (messy/handwritten).
        // If you want to force a good response for testing, you can change this random logic.
        boolean isGoodQuality = new Random().nextInt(10) > 0; // 90% chance of success

        if (!isGoodQuality) {
            response.setQualityGood(false);
            response.setMessage("Upload Failed: The image appears to be handwritten or blurry. We only accept official, printed university exam papers.");
            return response;
        }

        // Mocked Extracted Data
        response.setQualityGood(true);
        response.setSubject("Network Security");
        response.setSemester(8);
        response.setBranch("IMCA");
        response.setYear("2025");
        response.setMessage("Success");

        return response;
    }
}
