package com.devsop.project.apartmentinvoice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.devsop.project.apartmentinvoice.entity.Maintenance.Status;

import lombok.Data;

@Data
public class UpdateMaintenanceRequest {
  private String description;
  private LocalDate scheduledDate;
  private BigDecimal costBaht;
  private Status status;
  private LocalDate completedDate;
}
