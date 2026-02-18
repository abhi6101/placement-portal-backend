package com.abhi.authProject.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@Service
public class FileStorageService {

    @Value("${pdf.storage.directory:/tmp/resumes}")
    private String uploadDir;

    @Autowired
    private Cloudinary cloudinary;

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

    public String saveFile(MultipartFile file, String subDir) throws IOException {
        return saveFileFromStream(file.getInputStream(), file.getOriginalFilename(), subDir);
    }

    public String saveFileFromStream(InputStream inputStream, String originalFilename, String subDir)
            throws IOException {
        Path uploadPath = Paths.get(uploadDir, subDir);

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String fileName = System.currentTimeMillis() + "_"
                + originalFilename.replaceAll("[^a-zA-Z0-9.-]", "_");
        Path filePath = uploadPath.resolve(fileName);

        Files.copy(inputStream, filePath);

        return fileName;
    }

    public String savePaperToCloudinary(MultipartFile file) throws IOException {
        // Generate a unique public ID (filename) on Cloudinary
        String originalFilename = file.getOriginalFilename();
        String publicId = "papers/" + System.currentTimeMillis() + "_"
                + originalFilename.replaceAll("[^a-zA-Z0-9.-]", "_").replace(".pdf", "");

        Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap(
                        "public_id", publicId,
                        "resource_type", "auto", // Automatically detect PDF
                        "folder", "placement_portal_papers"));

        return (String) uploadResult.get("secure_url");
    }
}
