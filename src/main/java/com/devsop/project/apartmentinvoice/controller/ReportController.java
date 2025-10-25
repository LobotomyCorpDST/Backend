package com.devsop.project.apartmentinvoice.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.devsop.project.apartmentinvoice.dto.ReportSummaryDTO;
import com.devsop.project.apartmentinvoice.entity.Invoice;
import com.devsop.project.apartmentinvoice.entity.Room;
import com.devsop.project.apartmentinvoice.entity.Tenant;
import com.devsop.project.apartmentinvoice.repository.InvoiceRepository;
import com.devsop.project.apartmentinvoice.repository.RoomRepository;
import com.devsop.project.apartmentinvoice.repository.TenantRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

  private final InvoiceRepository invoiceRepo;
  private final RoomRepository roomRepo;
  private final TenantRepository tenantRepo;

  /**
   * Summary report for a specific room (all invoices)
   */
  @GetMapping("/by-room/{roomId}")
  public ReportSummaryDTO byRoom(@PathVariable Long roomId) {
    Room room = roomRepo.findById(roomId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

    List<Invoice> invoices = invoiceRepo.findByRoom_Id(roomId);

    return buildSummary(
        "room",
        invoices,
        roomId,
        String.valueOf(room.getNumber()),
        null,
        null,
        null,
        null
    );
  }

  /**
   * Summary report for a specific room by room number (all invoices)
   */
  @GetMapping("/by-room-number/{roomNumber}")
  public ReportSummaryDTO byRoomNumber(@PathVariable Integer roomNumber) {
    Room room = roomRepo.findByNumber(roomNumber)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

    List<Invoice> invoices = invoiceRepo.findByRoom_Id(room.getId());

    return buildSummary(
        "room",
        invoices,
        room.getId(),
        String.valueOf(room.getNumber()),
        null,
        null,
        null,
        null
    );
  }

  /**
   * Summary report for a specific room in a specific month
   */
  @GetMapping("/by-room/{roomId}/month/{year}/{month}")
  public ReportSummaryDTO byRoomAndMonth(
      @PathVariable Long roomId,
      @PathVariable Integer year,
      @PathVariable Integer month
  ) {
    Room room = roomRepo.findById(roomId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

    List<Invoice> allRoomInvoices = invoiceRepo.findByRoom_Id(roomId);
    List<Invoice> filteredInvoices = allRoomInvoices.stream()
        .filter(inv -> inv.getBillingYear().equals(year) && inv.getBillingMonth().equals(month))
        .toList();

    return buildSummary(
        "room_month",
        filteredInvoices,
        roomId,
        String.valueOf(room.getNumber()),
        null,
        null,
        year,
        month
    );
  }

  /**
   * Summary report for a specific room by room number in a specific month
   */
  @GetMapping("/by-room-number/{roomNumber}/month/{year}/{month}")
  public ReportSummaryDTO byRoomNumberAndMonth(
      @PathVariable Integer roomNumber,
      @PathVariable Integer year,
      @PathVariable Integer month
  ) {
    Room room = roomRepo.findByNumber(roomNumber)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

    List<Invoice> allRoomInvoices = invoiceRepo.findByRoom_Id(room.getId());
    List<Invoice> filteredInvoices = allRoomInvoices.stream()
        .filter(inv -> inv.getBillingYear().equals(year) && inv.getBillingMonth().equals(month))
        .toList();

    return buildSummary(
        "room_month",
        filteredInvoices,
        room.getId(),
        String.valueOf(room.getNumber()),
        null,
        null,
        year,
        month
    );
  }

  /**
   * Summary report for a specific tenant (all invoices)
   */
  @GetMapping("/by-tenant/{tenantId}")
  public ReportSummaryDTO byTenant(@PathVariable Long tenantId) {
    Tenant tenant = tenantRepo.findById(tenantId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));

    List<Invoice> invoices = invoiceRepo.findByTenant_Id(tenantId);

    return buildSummary(
        "tenant",
        invoices,
        null,
        null,
        tenantId,
        tenant.getName(),
        null,
        null
    );
  }

  /**
   * Summary report for a specific month (all rooms)
   */
  @GetMapping("/by-month/{year}/{month}")
  public ReportSummaryDTO byMonth(@PathVariable Integer year, @PathVariable Integer month) {
    List<Invoice> invoices = invoiceRepo.findByBillingYearAndBillingMonth(year, month);

    return buildSummary(
        "month",
        invoices,
        null,
        null,
        null,
        null,
        year,
        month
    );
  }

  // ---------- Helper method ----------

  private ReportSummaryDTO buildSummary(
      String filterType,
      List<Invoice> invoices,
      Long roomId,
      String roomNumber,
      Long tenantId,
      String tenantName,
      Integer year,
      Integer month
  ) {
    if (invoices.isEmpty()) {
      return ReportSummaryDTO.builder()
          .filterType(filterType)
          .roomId(roomId)
          .roomNumber(roomNumber)
          .tenantId(tenantId)
          .tenantName(tenantName)
          .year(year)
          .month(month)
          .totalInvoices(0)
          .totalElectricityUnits(BigDecimal.ZERO)
          .totalWaterUnits(BigDecimal.ZERO)
          .totalRentBaht(BigDecimal.ZERO)
          .totalElectricityBaht(BigDecimal.ZERO)
          .totalWaterBaht(BigDecimal.ZERO)
          .totalMaintenanceBaht(BigDecimal.ZERO)
          .totalCommonFeeBaht(BigDecimal.ZERO)
          .totalGarbageFeeBaht(BigDecimal.ZERO)
          .totalOtherBaht(BigDecimal.ZERO)
          .grandTotalBaht(BigDecimal.ZERO)
          .avgElectricityRate(BigDecimal.ZERO)
          .avgWaterRate(BigDecimal.ZERO)
          .invoices(invoices)
          .build();
    }

    // Calculate totals
    BigDecimal totalElecUnits = sumField(invoices, Invoice::getElectricityUnits);
    BigDecimal totalWaterUnits = sumField(invoices, Invoice::getWaterUnits);
    BigDecimal totalRent = sumField(invoices, Invoice::getRentBaht);
    BigDecimal totalElecBaht = sumField(invoices, Invoice::getElectricityBaht);
    BigDecimal totalWaterBaht = sumField(invoices, Invoice::getWaterBaht);
    BigDecimal totalMaintenance = sumField(invoices, Invoice::getMaintenanceBaht);
    BigDecimal totalCommonFee = sumField(invoices, Invoice::getCommonFeeBaht);
    BigDecimal totalGarbageFee = sumField(invoices, Invoice::getGarbageFeeBaht);
    BigDecimal totalOther = sumField(invoices, Invoice::getOtherBaht);
    BigDecimal grandTotal = sumField(invoices, Invoice::getTotalBaht);

    // Calculate average rates
    BigDecimal avgElecRate = calculateAvgRate(invoices, Invoice::getElectricityRate);
    BigDecimal avgWaterRate = calculateAvgRate(invoices, Invoice::getWaterRate);

    return ReportSummaryDTO.builder()
        .filterType(filterType)
        .roomId(roomId)
        .roomNumber(roomNumber)
        .tenantId(tenantId)
        .tenantName(tenantName)
        .year(year)
        .month(month)
        .totalInvoices(invoices.size())
        .totalElectricityUnits(totalElecUnits)
        .totalWaterUnits(totalWaterUnits)
        .totalRentBaht(totalRent)
        .totalElectricityBaht(totalElecBaht)
        .totalWaterBaht(totalWaterBaht)
        .totalMaintenanceBaht(totalMaintenance)
        .totalCommonFeeBaht(totalCommonFee)
        .totalGarbageFeeBaht(totalGarbageFee)
        .totalOtherBaht(totalOther)
        .grandTotalBaht(grandTotal)
        .avgElectricityRate(avgElecRate)
        .avgWaterRate(avgWaterRate)
        .invoices(invoices)
        .build();
  }

  private BigDecimal sumField(List<Invoice> invoices, java.util.function.Function<Invoice, BigDecimal> getter) {
    return invoices.stream()
        .map(getter)
        .filter(val -> val != null)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private BigDecimal calculateAvgRate(List<Invoice> invoices, java.util.function.Function<Invoice, BigDecimal> getter) {
    List<BigDecimal> rates = invoices.stream()
        .map(getter)
        .filter(val -> val != null && val.compareTo(BigDecimal.ZERO) > 0)
        .toList();

    if (rates.isEmpty()) {
      return BigDecimal.ZERO;
    }

    BigDecimal sum = rates.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    return sum.divide(BigDecimal.valueOf(rates.size()), 2, RoundingMode.HALF_UP);
  }
}
