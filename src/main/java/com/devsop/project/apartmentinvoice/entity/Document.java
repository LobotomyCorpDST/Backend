package com.devsop.project.apartmentinvoice.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Stores metadata for uploaded documents (contracts, receipts, maintenance reports).
 * Files are stored on filesystem, this entity tracks file paths and metadata.
 */
@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Document {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * Type of entity this document belongs to (LEASE, MAINTENANCE, INVOICE).
   */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @NotNull
  private EntityType entityType;

  /**
   * ID of the entity this document belongs to.
   * Example: If entityType=LEASE, this is the Lease ID.
   */
  @Column(nullable = false)
  @NotNull
  private Long entityId;

  /**
   * Original filename uploaded by user.
   */
  @Column(nullable = false)
  @NotNull
  private String fileName;

  /**
   * File path on disk (relative to upload directory).
   * Example: "lease/123/contract_20241028.pdf"
   */
  @Column(length = 500, nullable = false)
  @NotNull
  private String filePath;

  /**
   * File size in bytes.
   */
  @Column
  private Long fileSize;

  /**
   * MIME type (e.g., "image/jpeg", "application/pdf").
   */
  @Column(length = 100)
  private String mimeType;

  /**
   * Username of person who uploaded the file.
   */
  @Column
  private String uploadedBy;

  @Column(nullable = false)
  private LocalDateTime uploadedAt;

  @PrePersist
  protected void onCreate() {
    uploadedAt = LocalDateTime.now();
  }

  public enum EntityType {
    LEASE,      // สัญญาเช่า
    MAINTENANCE, // รายงาน/ใบเสร็จซ่อมบำรุง
    INVOICE     // ใบเสร็จการโอนเงิน/สลิปชำระเงิน
  }
}
