package com.devsop.project.apartmentinvoice.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.devsop.project.apartmentinvoice.entity.Document;
import com.devsop.project.apartmentinvoice.entity.Document.EntityType;
import com.devsop.project.apartmentinvoice.service.DocumentService;

import lombok.RequiredArgsConstructor;

/**
 * Controller for document upload/download/delete operations.
 * Supports attaching documents to Lease, Maintenance, and Invoice entities.
 */
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

  private final DocumentService documentService;

  /**
   * Upload a document file.
   *
   * @param file MultipartFile to upload
   * @param entityType Type of entity (LEASE, MAINTENANCE, INVOICE)
   * @param entityId ID of the entity
   * @param principal Authenticated user
   * @return Created Document entity
   */
  @PostMapping("/upload")
  public Document uploadDocument(
      @RequestParam("file") MultipartFile file,
      @RequestParam("entityType") String entityType,
      @RequestParam("entityId") Long entityId,
      Principal principal
  ) {
    EntityType type;
    try {
      type = EntityType.valueOf(entityType.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "Invalid entity type. Must be LEASE, MAINTENANCE, or INVOICE"
      );
    }

    String username = (principal != null) ? principal.getName() : "admin";

    return documentService.uploadDocument(file, type, entityId, username);
  }

  /**
   * Get all documents for a specific entity.
   *
   * @param entityType Type of entity
   * @param entityId ID of the entity
   * @return List of documents
   */
  @GetMapping("/{entityType}/{entityId}")
  public List<Document> getDocumentsByEntity(
      @PathVariable String entityType,
      @PathVariable Long entityId
  ) {
    EntityType type;
    try {
      type = EntityType.valueOf(entityType.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "Invalid entity type. Must be LEASE, MAINTENANCE, or INVOICE"
      );
    }

    return documentService.getDocumentsByEntity(type, entityId);
  }

  /**
   * [ปรับปรุง] Download by redirecting to a GCS Signed URL.
   */
  @GetMapping("/{id}/download")
  public ResponseEntity<Void> downloadDocument(@PathVariable Long id) {
    Document document = documentService.getDocumentById(id);
    String signedUrl = documentService.generateSignedUrlForDownload(id);

    HttpHeaders headers = new HttpHeaders();
    headers.setLocation(java.net.URI.create(signedUrl));
    headers.add(HttpHeaders.CONTENT_DISPOSITION,
        "attachment; filename=\"" + document.getFileName() + "\"");

    return new ResponseEntity<>(headers, HttpStatus.FOUND);
  }

  /**
   * Delete a document and its file.
   *
   * @param id Document ID
   * @return Success message
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<?> deleteDocument(@PathVariable Long id) {
    documentService.deleteDocument(id);
    return ResponseEntity.ok(java.util.Map.of("message", "Document deleted successfully"));
  }
}
