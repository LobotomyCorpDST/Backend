package com.devsop.project.apartmentinvoice.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.devsop.project.apartmentinvoice.entity.Document;
import com.devsop.project.apartmentinvoice.entity.Document.EntityType;
import com.devsop.project.apartmentinvoice.repository.DocumentRepository;
import com.devsop.project.apartmentinvoice.service.storage.StorageService;

/**
 * Service for managing document uploads, downloads, and deletions.
 * Uses StorageService abstraction for pluggable storage backends (local or GCS).
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
  private final StorageService storageService;

  public DocumentService(
      DocumentRepository documentRepository,
      StorageService storageService) {
    this.documentRepository = documentRepository;
    this.storageService = storageService;
  }

  /**
   * Upload a file and create a Document record.
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
    String filePath = String.format("%s/%d/%s",
        entityType.name().toLowerCase(), entityId, uniqueFilename);

    try {
      // Upload file using storage service
      storageService.uploadFile(file.getInputStream(), uniqueFilename, contentType, filePath);

      Document document = new Document();
      document.setEntityType(entityType);
      document.setEntityId(entityId);
      document.setFileName(originalFilename);
      document.setFilePath(filePath);
      document.setFileSize(file.getSize());
      document.setMimeType(contentType);
      document.setUploadedBy(uploadedBy);

      return documentRepository.save(document);
    } catch (IOException e) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Failed to upload file: " + e.getMessage(),
          e);
    }
  }

  /**
   * Read file content from storage.
   */
  public byte[] getFileContent(Long documentId) {
    Document document = documentRepository.findById(documentId)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Document not found: " + documentId));

    return storageService.downloadFile(document.getFilePath());
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
   * Delete a document and its file from storage.
   */
  public void deleteDocument(Long documentId) {
    Document document = documentRepository.findById(documentId)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Document not found: " + documentId));

    try {
      storageService.deleteFile(document.getFilePath());
    } catch (Exception e) {
      System.err.println("Warning: Failed to delete file from storage: " + e.getMessage());
    }

    documentRepository.deleteById(documentId);
  }
}
