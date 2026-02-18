package com.abhi.authProject.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.Permission;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.http.HttpCredentialsAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${pdf.storage.directory:/tmp/resumes}")
    private String localUploadDir;

    @Value("${google.drive.folder.id}")
    private String driveFolderId;

    private Drive driveService;

    @PostConstruct
    public void init() {
        try {
            // Setup Google Drive Service
            JsonFactory jsonFactory = GsonFactory.getDefaultInstance();
            NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

            // Load credentials from Environment Variable (Production) or File (Local Dev)
            InputStream credentialsStream;
            String envCredentials = System.getenv("GOOGLE_CREDENTIALS_JSON");

            if (envCredentials != null && !envCredentials.isEmpty()) {
                credentialsStream = new java.io.ByteArrayInputStream(envCredentials.getBytes());
            } else {
                // Fallback to local file
                credentialsStream = new ClassPathResource("google-drive-credentials.json").getInputStream();
            }

            GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream)
                    .createScoped(Collections.singleton(DriveScopes.DRIVE_FILE));

            HttpCredentialsAdapter requestInitializer = new HttpCredentialsAdapter(credentials);

            this.driveService = new Drive.Builder(httpTransport, jsonFactory, requestInitializer)
                    .setApplicationName("Placement-Portal")
                    .build();

            // Create local dir if not exists
            Files.createDirectories(Paths.get(localUploadDir));

        } catch (Exception e) {
            // e.printStackTrace(); // Optional
            System.err.println("Failed to initialize Google Drive Service: " + e.getMessage());
            // Don't throw exception here to allow app to start even if Drive fails (users
            // can still login etc)
        }
    }

    /**
     * Uploads a file to Google Drive and returns the Web View Link (Preview URL).
     * 
     * @param multipartFile The file to upload
     * @return The public preview URL
     */
    public String uploadFileToDrive(MultipartFile multipartFile) throws IOException {
        if (driveService == null) {
            throw new IOException("Google Drive Service not initialized. Check credentials and folder ID.");
        }

        File fileMetadata = new File();
        fileMetadata.setName(multipartFile.getOriginalFilename());
        // Upload to specific folder
        if (driveFolderId != null && !driveFolderId.isEmpty() && !driveFolderId.contains("YOUR_FOLDER_ID")) {
            fileMetadata.setParents(Collections.singletonList(driveFolderId));
        }

        File uploadedFile;
        try {
            InputStreamContent mediaContent = new InputStreamContent(
                    multipartFile.getContentType(),
                    multipartFile.getInputStream());

            uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                    .setFields("id, webViewLink, webContentLink")
                    .execute();
        } catch (com.google.api.client.googleapis.json.GoogleJsonResponseException e) {
            if (e.getStatusCode() == 404) {
                System.err.println("❌ Folder not found or permission denied: " + driveFolderId);
                System.out.println("⚠️ Falling back to upload to Root Directory.");

                // Clear parent (fallback to root)
                fileMetadata.setParents(null);

                // Re-open stream for retry (prevent Stream Closed)
                InputStreamContent mediaContentRetry = new InputStreamContent(
                        multipartFile.getContentType(),
                        multipartFile.getInputStream());

                uploadedFile = driveService.files().create(fileMetadata, mediaContentRetry)
                        .setFields("id, webViewLink, webContentLink")
                        .execute();
            } else {
                throw e;
            }
        }

        // Make Public (Visible to anyone with link - Reader access)
        Permission permission = new Permission()
                .setType("anyone")
                .setRole("reader");

        driveService.permissions().create(uploadedFile.getId(), permission).execute();

        // Return the WebViewLink (for viewing in browser/iframe)
        return uploadedFile.getWebViewLink();
    }

    /**
     * ORIGINAL LOCAL STORAGE METHODS (Preserved for Resume Uploads if used)
     */
    public String saveFile(MultipartFile file, String subDir) throws IOException {
        Path uploadPath = Paths.get(localUploadDir, subDir);
        Files.createDirectories(uploadPath);

        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return fileName;
    }

    public String saveFile(MultipartFile file) throws IOException {
        return saveFile(file, "");
    }

    public Path load(String filename) {
        return Paths.get(localUploadDir).resolve(filename);
    }

    // --- Compatibility Methods ---

    public String saveResume(MultipartFile file, String subDir) throws IOException {
        return saveFile(file, subDir);
    }

    public String savePaperToCloudinary(MultipartFile file) throws IOException {
        return uploadFileToDrive(file);
    }

    public String saveFileFromStream(InputStream inputStream, String originalFilename, String subDir)
            throws IOException {
        // For Bulk Upload: Upload directly to Drive
        if (driveService == null) {
            throw new IOException("Google Drive Service not initialized.");
        }

        // Create File Metadata
        File fileMetadata = new File();
        fileMetadata.setName(originalFilename);
        if (driveFolderId != null && !driveFolderId.isEmpty() && !driveFolderId.contains("YOUR_FOLDER_ID")) {
            fileMetadata.setParents(Collections.singletonList(driveFolderId));
        }

        // Assume PDF for bulk upload context, or stream generic
        InputStreamContent mediaContent = new InputStreamContent("application/pdf", inputStream);

        File uploadedFile;
        try {
            // Attempt upload to specified folder
            uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                    .setFields("id, webViewLink, webContentLink")
                    .execute();
        } catch (com.google.api.client.googleapis.json.GoogleJsonResponseException e) {
            if (e.getStatusCode() == 404) {
                System.err.println("❌ Folder not found or permission denied: " + driveFolderId);
                System.out.println("⚠️ Falling back to upload to Root Directory.");

                // Clear parent (fallback to root)
                fileMetadata.setParents(null);
                uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                        .setFields("id, webViewLink, webContentLink")
                        .execute();
            } else {
                throw e;
            }
        }

        // Make Public
        Permission permission = new Permission().setType("anyone").setRole("reader");
        driveService.permissions().create(uploadedFile.getId(), permission).execute();

        return uploadedFile.getWebViewLink();
    }
}
