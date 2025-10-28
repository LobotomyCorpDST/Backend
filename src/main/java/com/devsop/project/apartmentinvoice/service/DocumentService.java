package com.devsop.project.apartmentinvoice.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.devsop.project.apartmentinvoice.entity.Document;
import com.devsop.project.apartmentinvoice.entity.Document.EntityType;
import com.devsop.project.apartmentinvoice.repository.DocumentRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service for managing document uploads, downloads, and deletions.
 * Files are stored on local filesystem, metadata tracked in database.
 */
@Service
@RequiredArgsConstructor
public class DocumentService {

  private final DocumentRepository documentRepository;

  @Value("${file.upload.dir:./uploads}")
  private String uploadBaseDir;

  private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

  private static final List<String> ALLOWED_MIME_TYPES = List.of(
    "image/jpeg",
    "image/png",
    "image/jpg",
    "application/pdf"
  );

  /**
   * Upload a file and create a Document record.
   *
   * @param file MultipartFile from HTTP request
   * @param entityType Type of entity (LEASE, MAINTENANCE, INVOICE)
   * @param entityId ID of the entity
   * @param uploadedBy Username of uploader
   * @return Created Document entity
   */
  public Document uploadDocument(
      MultipartFile file,
      EntityType entityType,
      Long entityId,
      String uploadedBy
  ) {
    // Validate file
    if (file.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
    }

    if (file.getSize() > MAX_FILE_SIZE) {
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "File size exceeds maximum limit of 10MB"
      );
    }

    String contentType = file.getContentType();
    if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "File type not allowed. Only JPG, PNG, and PDF files are accepted."
      );
    }

    // Generate unique filename
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

    // Build file path: uploads/{entityType}/{entityId}/{uniqueFilename}
    String relativePath = String.format(
      "%s/%d/%s",
      entityType.name().toLowerCase(),
      entityId,
      uniqueFilename
    );

    Path uploadPath = Paths.get(uploadBaseDir, entityType.name().toLowerCase(), entityId.toString());
    Path filePath = uploadPath.resolve(uniqueFilename);

    try {
      // Create directories if they don't exist
      Files.createDirectories(uploadPath);

      // Save file
      Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

      // Create Document record
      Document document = new Document();
      document.setEntityType(entityType);
      document.setEntityId(entityId);
      document.setFileName(originalFilename);
      document.setFilePath(relativePath);
      document.setFileSize(file.getSize());
      document.setMimeType(contentType);
      document.setUploadedBy(uploadedBy);

      return documentRepository.save(document);

    } catch (IOException e) {
      throw new ResponseStatusException(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Failed to store file: " + e.getMessage()
      );
    }
  }

  /**
   * Get file content as byte array for download.
   *
   * @param documentId Document ID
   * @return File content as byte array
   */
  public byte[] getFileContent(Long documentId) {
    Document document = documentRepository.findById(documentId)
      .orElseThrow(() -> new ResponseStatusException(
        HttpStatus.NOT_FOUND,
        "Document not found: " + documentId
      ));

    Path filePath = Paths.get(uploadBaseDir, document.getFilePath());

    try {
      return Files.readAllBytes(filePath);
    } catch (IOException e) {
      throw new ResponseStatusException(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Failed to read file: " + e.getMessage()
      );
    }
  }

  /**
   * Get document metadata by ID.
   */
  public Document getDocumentById(Long id) {
    return documentRepository.findById(id)
      .orElseThrow(() -> new ResponseStatusException(
        HttpStatus.NOT_FOUND,
        "Document not found: " + id
      ));
  }

  /**
   * Get all documents for a specific entity.
   */
  public List<Document> getDocumentsByEntity(EntityType entityType, Long entityId) {
    return documentRepository.findByEntityTypeAndEntityId(entityType, entityId);
  }

  /**
   * Delete a document and its file from disk.
   */
  public void deleteDocument(Long documentId) {
    Document document = documentRepository.findById(documentId)
      .orElseThrow(() -> new ResponseStatusException(
        HttpStatus.NOT_FOUND,
        "Document not found: " + documentId
      ));

    Path filePath = Paths.get(uploadBaseDir, document.getFilePath());

    try {
      // Delete file from disk
      Files.deleteIfExists(filePath);
    } catch (IOException e) {
      System.err.println("Warning: Failed to delete file from disk: " + e.getMessage());
      // Continue to delete DB record even if file deletion fails
    }

    // Delete database record
    documentRepository.deleteById(documentId);
  }
}
