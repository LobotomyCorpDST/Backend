package com.devsop.project.apartmentinvoice.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for room-level trend data
 * Used for displaying graphs for each room owned by a tenant
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomTrendDTO {

  private Long roomId;
  private Integer roomNumber;
  private List<MonthlyTrendDTO> monthlyTrends;
}
