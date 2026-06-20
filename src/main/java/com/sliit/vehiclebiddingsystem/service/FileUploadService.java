package com.sliit.vehiclebiddingsystem.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileUploadService {

    private static final String UPLOAD_DIR = "src/main/resources/static/images/listings/";
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final List<String> ALLOWED_EXTENSIONS = List.of(".jpg", ".jpeg", ".png", ".gif", ".webp");

    /**
     * Upload multiple files and return their URLs
     */
    public List<String> uploadFiles(MultipartFile[] files) throws IOException {
        List<String> uploadedUrls = new ArrayList<>();
        
        if (files == null || files.length == 0) {
            return uploadedUrls;
        }

        // Ensure upload directory exists
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }

            // Validate file
            validateFile(file);

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String uniqueFilename = UUID.randomUUID().toString() + extension;

            // Save file
            Path filePath = uploadPath.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Add URL to list
            String fileUrl = "/images/listings/" + uniqueFilename;
            uploadedUrls.add(fileUrl);
        }

        return uploadedUrls;
    }

    /**
     * Upload a single file and return its URL
     */
    public String uploadFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        List<String> urls = uploadFiles(new MultipartFile[]{file});
        return urls.isEmpty() ? null : urls.get(0);
    }

    /**
     * Validate uploaded file
     */
    private void validateFile(MultipartFile file) throws IOException {
        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IOException("File size exceeds maximum allowed size of 5MB");
        }

        // Check file extension
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IOException("Invalid file name");
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IOException("File type not allowed. Only JPG, JPEG, PNG, GIF, and WebP files are allowed");
        }

        // Check content type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IOException("Invalid file type. Only image files are allowed");
        }
    }

    /**
     * Get file extension from filename
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        
        return filename.substring(lastDotIndex);
    }

    /**
     * Delete a file by URL
     */
    public boolean deleteFile(String fileUrl) {
        try {
            if (fileUrl == null || !fileUrl.startsWith("/images/listings/")) {
                return false;
            }

            String filename = fileUrl.substring("/images/listings/".length());
            Path filePath = Paths.get(UPLOAD_DIR + filename);
            
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            return false;
        }
    }
}
