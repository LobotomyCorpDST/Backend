package com.devsop.project.apartmentinvoice.service;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.devsop.project.apartmentinvoice.entity.Document;
import com.devsop.project.apartmentinvoice.entity.Document.EntityType;
import com.devsop.project.apartmentinvoice.repository.DocumentRepository;

/**
 * Service for managing document uploads, downloads, deletions, and signed URLs using GCS.
 */
@Service
public class DocumentService {

  private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
  private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
      "image/jpeg",
      "image/png",
      "image/jpg",
      "application/pdf");

  private final DocumentRepository documentRepository;
  private final Storage storage;
  private final String bucketName;

  public DocumentService(
      DocumentRepository documentRepository,
      @Value("${gcs.bucket.name}") String bucketName) {
    this.documentRepository = documentRepository;
    this.bucketName = bucketName;
    this.storage = StorageOptions.getDefaultInstance().getService();
  }

  /**
   * Upload a file to GCS and create a Document record.
   */
  public Document uploadDocument(
      MultipartFile file,
      EntityType entityType,
      Long entityId,
      String uploadedBy) {
    if (file.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
    }

    if (file.getSize() > MAX_FILE_SIZE) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "File size exceeds maximum limit of 10MB");
    }

    String contentType = file.getContentType();
    if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "File type not allowed. Only JPG, PNG, and PDF files are accepted.");
    }

    String originalFilename = file.getOriginalFilename();
    if (originalFilename == null || originalFilename.isEmpty()) {
      originalFilename = "file";
    }

    String extension = "";
    int dotIndex = originalFilename.lastIndexOf('.');
    if (dotIndex > 0) {
      extension = originalFilename.substring(dotIndex);
    }

    String uniqueFilename = UUID.randomUUID().toString() + extension;
    String gcsObjectName = String.format("%s/%d/%s",
        entityType.name().toLowerCase(), entityId, uniqueFilename);

    try {
      BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, gcsObjectName))
          .setContentType(contentType)
          .build();

      storage.createFrom(blobInfo, file.getInputStream());

      Document document = new Document();
      document.setEntityType(entityType);
      document.setEntityId(entityId);
      document.setFileName(originalFilename);
      document.setFilePath(gcsObjectName);
      document.setFileSize(file.getSize());
      document.setMimeType(contentType);
      document.setUploadedBy(uploadedBy);

      return documentRepository.save(document);
    } catch (IOException e) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Failed to upload file to GCS: " + e.getMessage(),
          e);
    }
  }

  /**
   * Read file content from GCS.
   */
  public byte[] getFileContent(Long documentId) {
    Document document = documentRepository.findById(documentId)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Document not found: " + documentId));

    String gcsObjectName = document.getFilePath();
    try {
      BlobId blobId = BlobId.of(bucketName, gcsObjectName);
      byte[] content = storage.readAllBytes(blobId);
      if (content == null) {
        throw new IOException("GCS object not found or empty.");
      }
      return content;
    } catch (Exception e) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Failed to read file from GCS: " + e.getMessage(),
          e);
    }
  }

  /**
   * Get document metadata by ID.
   */
  public Document getDocumentById(Long id) {
    return documentRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Document not found: " + id));
  }

  /**
   * Get all documents for an entity.
   */
  public List<Document> getDocumentsByEntity(EntityType entityType, Long entityId) {
    return documentRepository.findByEntityTypeAndEntityId(entityType, entityId);
  }

  /**
   * Generate a short-lived signed URL for direct download from GCS.
   */
  public String generateSignedUrlForDownload(Long documentId) {
    Document document = getDocumentById(documentId);
    String gcsObjectName = document.getFilePath();
    try {
      BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, gcsObjectName)).build();
      URL signedUrl = storage.signUrl(
          blobInfo,
          10,
          TimeUnit.MINUTES,
          Storage.SignUrlOption.withV4Signature());
      return signedUrl.toString();
    } catch (Exception e) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Failed to generate signed URL: " + e.getMessage(),
          e);
    }
  }

  /**
   * Delete a document and its file from GCS.
   */
  public void deleteDocument(Long documentId) {
    Document document = documentRepository.findById(documentId)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Document not found: " + documentId));

    String gcsObjectName = document.getFilePath();
    BlobId blobId = BlobId.of(bucketName, gcsObjectName);
    try {
      boolean deleted = storage.delete(blobId);
      if (!deleted) {
        System.err.println("Warning: GCS Object not found or deletion failed for: " + gcsObjectName);
      }
    } catch (Exception e) {
      System.err.println("Warning: Failed to delete GCS object: " + e.getMessage());
    }

    documentRepository.deleteById(documentId);
  }
}

