package com.abhi.authProject.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class FileStorageService {

    @Value("${pdf.storage.directory:/tmp/resumes}")
    private String uploadDir;

    public String saveResume(MultipartFile file, String applicantName) throws IOException {
        Path uploadPath = Paths.get(uploadDir);

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String fileName = generateUniqueFileName(file, applicantName);
        Path filePath = uploadPath.resolve(fileName);

        Files.copy(file.getInputStream(), filePath);

        return filePath.toAbsolutePath().toString();
    }

    private String generateUniqueFileName(MultipartFile file, String applicantName) {
        String originalFilename = file.getOriginalFilename();
        String extension = "";

        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String sanitizedName = applicantName.replaceAll("[^a-zA-Z0-9.-]", "_");
        return sanitizedName + "_" + System.currentTimeMillis() + extension;
    }
}
