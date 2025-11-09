package com.devsop.project.apartmentinvoice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateMaintenanceRequest {
  @NotNull
  private Integer roomNumber;

  @NotBlank
  private String description;

  private LocalDate scheduledDate;
  private BigDecimal costBaht;
  private String responsiblePerson;
  private String responsiblePhone;
}
