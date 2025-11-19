package com.devsop.project.apartmentinvoice.service.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Local file system implementation of StorageService.
 * Used in development environments where files are stored in the local filesystem.
 */
@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    private final Path rootLocation;

    public LocalStorageService(@Value("${storage.local-path:/app/uploads}") String uploadPath) {
        this.rootLocation = Paths.get(uploadPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory: " + rootLocation, e);
        }
    }

    @Override
    public String uploadFile(InputStream inputStream, String fileName, String contentType, String filePath) {
        try {
            Path targetLocation = rootLocation.resolve(filePath);

            // Create parent directories if they don't exist
            Files.createDirectories(targetLocation.getParent());

            // Copy file to target location
            Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return filePath;
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to store file: " + e.getMessage(),
                    e);
        }
    }

    @Override
    public byte[] downloadFile(String filePath) {
        try {
            Path file = rootLocation.resolve(filePath).normalize();

            // Security check: ensure the file is within the root location
            if (!file.startsWith(rootLocation)) {
                throw new SecurityException("Cannot access file outside upload directory");
            }

            if (!Files.exists(file)) {
                throw new IOException("File not found: " + filePath);
            }

            return Files.readAllBytes(file);
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "File not found: " + filePath,
                    e);
        } catch (SecurityException e) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Access denied: " + e.getMessage(),
                    e);
        }
    }

    @Override
    public void deleteFile(String filePath) {
        try {
            Path file = rootLocation.resolve(filePath).normalize();

            // Security check: ensure the file is within the root location
            if (!file.startsWith(rootLocation)) {
                throw new SecurityException("Cannot delete file outside upload directory");
            }

            if (Files.exists(file)) {
                Files.delete(file);
            }
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to delete file: " + e.getMessage(),
                    e);
        } catch (SecurityException e) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Access denied: " + e.getMessage(),
                    e);
        }
    }

    @Override
    public boolean fileExists(String filePath) {
        try {
            Path file = rootLocation.resolve(filePath).normalize();

            // Security check: ensure the file is within the root location
            if (!file.startsWith(rootLocation)) {
                return false;
            }

            return Files.exists(file);
        } catch (Exception e) {
            return false;
        }
    }
}
