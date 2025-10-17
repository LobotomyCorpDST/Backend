package com.devsop.project.apartmentinvoice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.devsop.project.apartmentinvoice.entity.Maintenance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateMaintenanceRequest {

  @NotNull
  private Integer roomNumber;   // ใช้ roomNumber แทน id

  @NotBlank
  private String description;   // ใช้ @NotBlank ดีกว่า เพราะไม่อยากให้เป็น ""

  @NotNull
  private LocalDate scheduledDate;

  private BigDecimal costBaht;

  // optional: ใช้ตอน update (เช่น PATCH)
  private Maintenance.Status status;
}
