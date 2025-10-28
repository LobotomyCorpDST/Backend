package com.devsop.project.apartmentinvoice.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Inventory tracking for apartment supplies/furniture.
 * Examples: เก้าอี้ (chairs), ตู้เย็น (refrigerators), หลอดไฟ (light bulbs), ก๊อกน้ำ (faucets)
 */
@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Supply {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * Name of the supply item (e.g., "เก้าอี้", "ตู้เย็น", "หลอดไฟ").
   */
  @Column(nullable = false)
  @NotNull
  private String supplyName;

  /**
   * Current quantity in stock.
   * Low inventory alert when < 3.
   */
  @Column(nullable = false)
  @NotNull
  private Integer supplyAmount = 0;

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

  /**
   * Helper method to check if inventory is low (< 3 units).
   */
  public boolean isLowStock() {
    return supplyAmount != null && supplyAmount < 3;
  }
}
