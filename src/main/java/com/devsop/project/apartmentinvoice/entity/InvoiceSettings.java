package com.devsop.project.apartmentinvoice.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Singleton entity for apartment-wide invoice settings.
 * Only one row should exist (id=1).
 * Contains payment information displayed on invoices: QR code, bank details, interest rate.
 */
@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class InvoiceSettings {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * Payment description shown on invoices (bank details, transfer instructions).
   * Example: "ธนาคารกสิกรไทย\nบัญชีออมทรัพย์\nเลขที่ 123-4-56789-0\nชื่อบัญชี: อพาร์ทเมนต์ABC"
   */
  @Column(columnDefinition = "TEXT")
  private String paymentDescription;

  /**
   * File path to QR code image for payment (relative to upload directory).
   * Example: "qr/payment_qr.png"
   */
  @Column(length = 500)
  private String qrCodeImagePath;

  /**
   * Monthly interest rate for late payments (percentage).
   * Example: 2.00 = 2% per month
   */
  @Column(precision = 5, scale = 2)
  private BigDecimal interestRatePerMonth;

  @Column
  private LocalDateTime createdAt;

  @Column
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
