package com.devsop.project.apartmentinvoice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.devsop.project.apartmentinvoice.entity.Maintenance.Status;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class MaintenanceResponse {
  private Long id;

  // ส่งแค่ข้อมูลห้องเท่าที่ต้องใช้ (เลี่ยง proxy)
  private Long roomId;
  private Integer roomNumber;

  private String description;
  private Status status;
  private LocalDate scheduledDate;
  private LocalDate completedDate;
  private BigDecimal costBaht;
  private String responsiblePerson;
  private String responsiblePhone;
}
