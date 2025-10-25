package com.devsop.project.apartmentinvoice.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyTrendDTO {

  private String month; // Format: "YYYY-MM" (e.g., "2025-01")
  private BigDecimal electricityUnits;
  private BigDecimal electricityBaht;
  private BigDecimal waterUnits;
  private BigDecimal waterBaht;
}
