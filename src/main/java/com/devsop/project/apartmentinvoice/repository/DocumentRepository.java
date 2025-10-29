package com.devsop.project.apartmentinvoice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.devsop.project.apartmentinvoice.entity.Document;
import com.devsop.project.apartmentinvoice.entity.Document.EntityType;

/**
 * Repository for Document entity.
 * Supports querying documents by entity type and ID.
 */
public interface DocumentRepository extends JpaRepository<Document, Long> {

  /**
   * Find all documents for a specific entity (e.g., all documents for Lease ID 5).
   */
  List<Document> findByEntityTypeAndEntityId(EntityType entityType, Long entityId);

  /**
   * Find all documents of a specific type (e.g., all LEASE documents).
   */
  List<Document> findByEntityType(EntityType entityType);
}
