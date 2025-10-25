package com.devsop.project.apartmentinvoice.dto;

import java.math.BigDecimal;
import java.util.List;

import com.devsop.project.apartmentinvoice.entity.Invoice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportSummaryDTO {

  // Metadata
  private String filterType; // "room", "tenant", "month", or "room_month"
  private Long roomId;
  private String roomNumber;
  private Long tenantId;
  private String tenantName;
  private Integer year;
  private Integer month;

  // Summary statistics
  private Integer totalInvoices;
  private BigDecimal totalElectricityUnits;
  private BigDecimal totalWaterUnits;
  private BigDecimal totalRentBaht;
  private BigDecimal totalElectricityBaht;
  private BigDecimal totalWaterBaht;
  private BigDecimal totalMaintenanceBaht;
  private BigDecimal totalCommonFeeBaht;
  private BigDecimal totalGarbageFeeBaht;
  private BigDecimal totalOtherBaht;
  private BigDecimal grandTotalBaht;

  // Average rates
  private BigDecimal avgElectricityRate;
  private BigDecimal avgWaterRate;

  // Detailed breakdown
  private List<Invoice> invoices;
}
