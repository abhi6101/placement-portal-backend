package com.abhi.authProject.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.FileContent;
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
                    .setSupportsAllDrives(true) // comprehensive support
                    .execute();

            // Disable downloading/copying/printing for viewers
            File patchMetadata = new File();
            patchMetadata.setViewersCanCopyContent(false);
            driveService.files().update(uploadedFile.getId(), patchMetadata).execute();
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
                        .setSupportsAllDrives(true)
                        .execute();

                // Disable downloading/copying/printing for viewers (Retry)
                File patchMetadata = new File();
                patchMetadata.setViewersCanCopyContent(false);
                driveService.files().update(uploadedFile.getId(), patchMetadata).execute();
            } else {
                throw e;
            }
        }

        // Do NOT make public. Keep restricted for secure streaming only.
        // Permission permission = new Permission().setType("anyone").setRole("reader");
        // driveService.permissions().create(uploadedFile.getId(),
        // permission).execute();

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

        // Buffer InputStream to a Temp File to allow retry
        java.io.File tempFile = java.nio.file.Files.createTempFile("upload-retry-", ".tmp").toFile();
        try (java.io.OutputStream os = new java.io.FileOutputStream(tempFile)) {
            inputStream.transferTo(os);
        }

        FileContent mediaContent = new FileContent("application/pdf", tempFile);
        File uploadedFile;

        try {
            // Attempt upload to specified folder
            uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                    .setFields("id, webViewLink, webContentLink")
                    .setSupportsAllDrives(true) // comprehensive support
                    .execute();

            // Disable downloading/copying/printing for viewers
            File patchMetadata = new File();
            patchMetadata.setViewersCanCopyContent(false);
            driveService.files().update(uploadedFile.getId(), patchMetadata).execute();
        } catch (com.google.api.client.googleapis.json.GoogleJsonResponseException e) {
            if (e.getStatusCode() == 404) {
                System.err.println("❌ Folder not found or permission denied: " + driveFolderId);
                System.out.println("⚠️ Falling back to upload to Root Directory.");

                // Clear parent (fallback to root)
                fileMetadata.setParents(null);

                // Reuse FileContent (it points to the same temp file)
                uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                        .setFields("id, webViewLink, webContentLink")
                        .setSupportsAllDrives(true) // comprehensive support
                        .execute();

                // Disable downloading/copying/printing for viewers (Bulk Retry)
                File patchMetadata = new File();
                patchMetadata.setViewersCanCopyContent(false);
                driveService.files().update(uploadedFile.getId(), patchMetadata).execute();
            } else {
                throw e;
            }
        } finally {
            // Cleanup Temp File
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }

        // Do NOT make public. Keep restricted for secure streaming only.
        // Permission permission = new Permission().setType("anyone").setRole("reader");
        // driveService.permissions().create(uploadedFile.getId(),
        // permission).execute();

        return uploadedFile.getWebViewLink();
    }

    /**
     * Securely streams file content from Google Drive.
     * Use this in a Controller endpoint secured with @PreAuthorize.
     */
    public java.io.InputStream getFileStream(String fileId) throws java.io.IOException {
        if (driveService == null) {
            init();
        }
        // Direct stream from Google Drive
        return driveService.files().get(fileId).executeMediaAsInputStream();
    }

    /**
     * Secures an existing file by:
     * 1. Disabling "Viewers can copy/download".
     * 2. Removing "Anyone with the link" permission (making it restricted).
     */
    public void secureFile(String fileId) throws java.io.IOException {
        if (driveService == null) {
            init();
        }

        try {
            // 1. Disable downloading/copying/printing
            File patchMetadata = new File();
            patchMetadata.setViewersCanCopyContent(false);
            driveService.files().update(fileId, patchMetadata).execute();

            // 2. Remove "anyone" permission (Make Restricted)
            // We need to list permissions first to find the ID of the "anyone" permission
            com.google.api.services.drive.model.PermissionList permissions = driveService.permissions().list(fileId)
                    .execute();
            if (permissions.getPermissions() != null) {
                for (Permission p : permissions.getPermissions()) {
                    if ("anyone".equals(p.getType())) {
                        System.out.println("Removing 'anyone' permission from file: " + fileId);
                        driveService.permissions().delete(fileId, p.getId()).execute();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error securing file " + fileId + ": " + e.getMessage());
        }
    }
}
