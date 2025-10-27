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
public class FloorSummaryDTO {

  private Integer floor;
  private Integer roomCount;
  private BigDecimal totalElectricityUnits;
  private BigDecimal totalElectricityBaht;
  private BigDecimal totalWaterUnits;
  private BigDecimal totalWaterBaht;
}
