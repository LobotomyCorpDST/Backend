package com.devsop.project.apartmentinvoice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ใช้สำหรับอัปเดตงานบำรุงรักษา (ไม่บังคับทุกฟิลด์)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMaintenanceRequest {
  private Integer roomNumber;     // ถ้าเปลี่ยนห้อง
  private String description;     // คำอธิบาย
  private LocalDate scheduledDate;
  private LocalDate completedDate;
  private BigDecimal costBaht;
  private String status;          // PLANNED / IN_PROGRESS / COMPLETED / CANCELED
  private String responsiblePerson;
  private String responsiblePhone;
}
