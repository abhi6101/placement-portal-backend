package com.abhi.authProject.service;

import com.abhi.authProject.model.Users;
import com.abhi.authProject.repo.UserRepo;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class ProfilePictureService {

    @Autowired
    private Cloudinary cloudinary;

    @Autowired
    private UserRepo userRepo;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final String[] ALLOWED_FORMATS = { "jpg", "jpeg", "png", "gif", "webp" };

    public String uploadProfilePicture(MultipartFile file, Users user) throws IOException {
        // Validate file
        validateImage(file);

        // Delete old picture if exists
        if (user.getProfilePictureUrl() != null && !user.getProfilePictureUrl().isEmpty()) {
            deleteProfilePicture(user);
        }

        // Upload to Cloudinary
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap(
                        "folder", "profile_pictures",
                        "public_id", "user_" + user.getId(),
                        "overwrite", true,
                        "resource_type", "image",
                        "transformation", new com.cloudinary.Transformation()
                                .width(500).height(500)
                                .crop("fill")
                                .gravity("face")
                                .quality("auto")));

        String imageUrl = (String) uploadResult.get("secure_url");

        // Update user
        user.setProfilePictureUrl(imageUrl);
        userRepo.save(user);

        return imageUrl;
    }

    public void deleteProfilePicture(Users user) throws IOException {
        if (user.getProfilePictureUrl() != null && !user.getProfilePictureUrl().isEmpty()) {
            // Extract public_id from URL
            String publicId = extractPublicId(user.getProfilePictureUrl());

            if (publicId != null) {
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            }

            // Update user
            user.setProfilePictureUrl(null);
            userRepo.save(user);
        }
    }

    private void validateImage(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IOException("File size exceeds maximum limit of 5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IOException("File must be an image");
        }

        String extension = getFileExtension(file.getOriginalFilename());
        boolean isAllowed = false;
        for (String format : ALLOWED_FORMATS) {
            if (format.equalsIgnoreCase(extension)) {
                isAllowed = true;
                break;
            }
        }

        if (!isAllowed) {
            throw new IOException("File format not supported. Allowed formats: jpg, jpeg, png, gif, webp");
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null)
            return "";
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1) : "";
    }

    private String extractPublicId(String imageUrl) {
        // Extract public_id from Cloudinary URL
        // Example URL:
        // https://res.cloudinary.com/cloud-name/image/upload/v1234567890/profile_pictures/user_123.jpg
        // Public ID: profile_pictures/user_123
        try {
            String[] parts = imageUrl.split("/upload/");
            if (parts.length > 1) {
                String pathAfterUpload = parts[1];
                // Remove version number (v1234567890/)
                String[] pathParts = pathAfterUpload.split("/", 2);
                if (pathParts.length > 1) {
                    String publicIdWithExtension = pathParts[1];
                    // Remove file extension
                    int lastDot = publicIdWithExtension.lastIndexOf('.');
                    return lastDot > 0 ? publicIdWithExtension.substring(0, lastDot) : publicIdWithExtension;
                }
            }
        } catch (Exception e) {
            System.err.println("Error extracting public_id: " + e.getMessage());
        }
        return null;
    }
}
