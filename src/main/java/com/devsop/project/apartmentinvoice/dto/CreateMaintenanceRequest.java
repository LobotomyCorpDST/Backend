package com.devsop.project.apartmentinvoice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateMaintenanceRequest {
  @NotNull
  private Long roomId;

  @NotNull
  private String description;

  @NotNull
  private LocalDate scheduledDate;

  private BigDecimal costBaht; // ไม่บังคับ
}
