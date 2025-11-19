package com.devsop.project.apartmentinvoice.service.storage;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Google Cloud Storage implementation of StorageService.
 * Used in production environments (GKE) with proper service account credentials.
 */
@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "gcs")
public class GcsStorageService implements StorageService {

    private final Storage storage;
    private final String bucketName;

    public GcsStorageService(@Value("${gcs.bucket.name}") String bucketName) {
        this.bucketName = bucketName;
        this.storage = StorageOptions.getDefaultInstance().getService();
    }

    @Override
    public String uploadFile(InputStream inputStream, String fileName, String contentType, String filePath) {
        try {
            BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, filePath))
                    .setContentType(contentType)
                    .build();

            storage.createFrom(blobInfo, inputStream);
            return filePath;
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to upload file to GCS: " + e.getMessage(),
                    e);
        }
    }

    @Override
    public byte[] downloadFile(String filePath) {
        try {
            BlobId blobId = BlobId.of(bucketName, filePath);
            byte[] content = storage.readAllBytes(blobId);
            if (content == null) {
                throw new IOException("GCS object not found or empty: " + filePath);
            }
            return content;
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to read file from GCS: " + e.getMessage(),
                    e);
        }
    }

    @Override
    public void deleteFile(String filePath) {
        try {
            BlobId blobId = BlobId.of(bucketName, filePath);
            boolean deleted = storage.delete(blobId);
            if (!deleted) {
                throw new IOException("Failed to delete file from GCS: " + filePath);
            }
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to delete file from GCS: " + e.getMessage(),
                    e);
        }
    }

    @Override
    public boolean fileExists(String filePath) {
        try {
            BlobId blobId = BlobId.of(bucketName, filePath);
            return storage.get(blobId) != null;
        } catch (Exception e) {
            return false;
        }
    }
}
