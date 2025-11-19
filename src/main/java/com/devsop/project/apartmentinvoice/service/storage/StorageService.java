package com.devsop.project.apartmentinvoice.service.storage;

import java.io.InputStream;

/**
 * Storage service interface for file upload/download operations.
 * Implementations can use different storage backends (local file system, Google Cloud Storage, etc.)
 */
public interface StorageService {

    /**
     * Upload a file to storage
     *
     * @param inputStream The file content as an input stream
     * @param fileName The name of the file
     * @param contentType The MIME type of the file
     * @param filePath The path where the file should be stored (e.g., "maintenance/300/file.png")
     * @return The storage path or URL of the uploaded file
     * @throws RuntimeException if upload fails
     */
    String uploadFile(InputStream inputStream, String fileName, String contentType, String filePath);

    /**
     * Download a file from storage
     *
     * @param filePath The path of the file to download
     * @return The file content as a byte array
     * @throws RuntimeException if download fails
     */
    byte[] downloadFile(String filePath);

    /**
     * Delete a file from storage
     *
     * @param filePath The path of the file to delete
     * @throws RuntimeException if deletion fails
     */
    void deleteFile(String filePath);

    /**
     * Check if a file exists in storage
     *
     * @param filePath The path of the file to check
     * @return true if the file exists, false otherwise
     */
    boolean fileExists(String filePath);
}
