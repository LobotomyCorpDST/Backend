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
public class RoomComparisonDTO {

  private Integer roomNumber;
  private BigDecimal electricityUnits;
  private BigDecimal electricityBaht;
  private BigDecimal waterUnits;
  private BigDecimal waterBaht;
  private Boolean isSelected;
}
