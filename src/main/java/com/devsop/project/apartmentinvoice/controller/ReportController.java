package com.devsop.project.apartmentinvoice.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.devsop.project.apartmentinvoice.dto.FloorSummaryDTO;
import com.devsop.project.apartmentinvoice.dto.MonthlyTrendDTO;
import com.devsop.project.apartmentinvoice.dto.ReportSummaryDTO;
import com.devsop.project.apartmentinvoice.dto.RoomComparisonDTO;
import com.devsop.project.apartmentinvoice.dto.RoomTrendDTO;
import com.devsop.project.apartmentinvoice.entity.Invoice;
import com.devsop.project.apartmentinvoice.entity.Lease;
import com.devsop.project.apartmentinvoice.entity.Room;
import com.devsop.project.apartmentinvoice.entity.Tenant;
import com.devsop.project.apartmentinvoice.repository.InvoiceRepository;
import com.devsop.project.apartmentinvoice.repository.LeaseRepository;
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
  private final LeaseRepository leaseRepo;

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

  // =================== NEW CHART ENDPOINTS ===================

  /**
   * Get floor comparison data for a specific month
   * Groups all rooms by floor and aggregates electricity/water usage
   */
  @GetMapping("/floor-comparison/{year}/{month}")
  public List<FloorSummaryDTO> getFloorComparison(
      @PathVariable Integer year,
      @PathVariable Integer month
  ) {
    // Get all invoices for the specified month
    List<Invoice> invoices = invoiceRepo.findByBillingYearAndBillingMonth(year, month);

    // Group by floor
    Map<Integer, List<Invoice>> byFloor = new HashMap<>();
    for (Invoice inv : invoices) {
      if (inv.getRoom() != null && inv.getRoom().getNumber() != null) {
        int floor = inv.getRoom().getNumber() / 100;
        byFloor.computeIfAbsent(floor, k -> new ArrayList<>()).add(inv);
      }
    }

    // Build floor summaries
    List<FloorSummaryDTO> result = new ArrayList<>();
    for (Map.Entry<Integer, List<Invoice>> entry : byFloor.entrySet()) {
      Integer floor = entry.getKey();
      List<Invoice> floorInvoices = entry.getValue();

      // Count unique rooms on this floor
      long roomCount = floorInvoices.stream()
          .map(inv -> inv.getRoom().getId())
          .distinct()
          .count();

      FloorSummaryDTO summary = FloorSummaryDTO.builder()
          .floor(floor)
          .roomCount((int) roomCount)
          .totalElectricityUnits(sumField(floorInvoices, Invoice::getElectricityUnits))
          .totalElectricityBaht(sumField(floorInvoices, Invoice::getElectricityBaht))
          .totalWaterUnits(sumField(floorInvoices, Invoice::getWaterUnits))
          .totalWaterBaht(sumField(floorInvoices, Invoice::getWaterBaht))
          .build();

      result.add(summary);
    }

    // Sort by floor number
    result.sort((a, b) -> a.getFloor().compareTo(b.getFloor()));
    return result;
  }

  /**
   * Get monthly trend data for a specific room
   */
  @GetMapping("/monthly-trend/{roomNumber}")
  public List<MonthlyTrendDTO> getMonthlyTrend(
      @PathVariable Integer roomNumber,
      @RequestParam(defaultValue = "6") Integer months
  ) {
    // Limit months to reasonable range
    if (months > 24) months = 24;
    if (months < 1) months = 1;

    Room room = roomRepo.findByNumber(roomNumber)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

    // Get all invoices for this room
    List<Invoice> allInvoices = invoiceRepo.findByRoom_Id(room.getId());

    // Group by year-month
    Map<String, List<Invoice>> byMonth = new HashMap<>();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");

    for (Invoice inv : allInvoices) {
      if (inv.getBillingYear() != null && inv.getBillingMonth() != null) {
        String key = String.format("%04d-%02d", inv.getBillingYear(), inv.getBillingMonth());
        byMonth.computeIfAbsent(key, k -> new ArrayList<>()).add(inv);
      }
    }

    // Build trend data for last N months
    List<MonthlyTrendDTO> result = new ArrayList<>();
    YearMonth current = YearMonth.now();

    for (int i = months - 1; i >= 0; i--) {
      YearMonth targetMonth = current.minusMonths(i);
      String key = targetMonth.format(formatter);

      List<Invoice> monthInvoices = byMonth.getOrDefault(key, new ArrayList<>());

      MonthlyTrendDTO trend = MonthlyTrendDTO.builder()
          .month(key)
          .electricityUnits(sumField(monthInvoices, Invoice::getElectricityUnits))
          .electricityBaht(sumField(monthInvoices, Invoice::getElectricityBaht))
          .waterUnits(sumField(monthInvoices, Invoice::getWaterUnits))
          .waterBaht(sumField(monthInvoices, Invoice::getWaterBaht))
          .build();

      result.add(trend);
    }

    return result;
  }

  /**
   * Get monthly trend data for an entire floor
   */
  @GetMapping("/floor-trend/{floor}")
  public List<MonthlyTrendDTO> getFloorTrend(
      @PathVariable Integer floor,
      @RequestParam(required = false) String startMonth,
      @RequestParam(required = false) String endMonth
  ) {
    // Default to last 6 months if no range specified
    YearMonth start, end;
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");

    if (startMonth != null && endMonth != null) {
      start = YearMonth.parse(startMonth, formatter);
      end = YearMonth.parse(endMonth, formatter);
    } else {
      end = YearMonth.now();
      start = end.minusMonths(5);
    }

    // Get all rooms on this floor
    List<Room> floorRooms = roomRepo.findAll().stream()
        .filter(r -> r.getNumber() != null && r.getNumber() / 100 == floor)
        .toList();

    List<Long> roomIds = floorRooms.stream().map(Room::getId).toList();

    // Get all invoices for these rooms
    List<Invoice> allInvoices = new ArrayList<>();
    for (Long roomId : roomIds) {
      allInvoices.addAll(invoiceRepo.findByRoom_Id(roomId));
    }

    // Group by year-month
    Map<String, List<Invoice>> byMonth = new HashMap<>();
    for (Invoice inv : allInvoices) {
      if (inv.getBillingYear() != null && inv.getBillingMonth() != null) {
        String key = String.format("%04d-%02d", inv.getBillingYear(), inv.getBillingMonth());
        byMonth.computeIfAbsent(key, k -> new ArrayList<>()).add(inv);
      }
    }

    // Build trend data for the date range
    List<MonthlyTrendDTO> result = new ArrayList<>();
    YearMonth current = start;

    while (!current.isAfter(end)) {
      String key = current.format(formatter);
      List<Invoice> monthInvoices = byMonth.getOrDefault(key, new ArrayList<>());

      MonthlyTrendDTO trend = MonthlyTrendDTO.builder()
          .month(key)
          .electricityUnits(sumField(monthInvoices, Invoice::getElectricityUnits))
          .electricityBaht(sumField(monthInvoices, Invoice::getElectricityBaht))
          .waterUnits(sumField(monthInvoices, Invoice::getWaterUnits))
          .waterBaht(sumField(monthInvoices, Invoice::getWaterBaht))
          .build();

      result.add(trend);
      current = current.plusMonths(1);
    }

    return result;
  }

  /**
   * Get room-level comparison data for the entire floor of a selected room
   * Returns usage data for all rooms on the same floor, with the selected room marked
   */
  @GetMapping("/floor-rooms-comparison/{roomNumber}")
  public List<RoomComparisonDTO> getFloorRoomsComparison(@PathVariable Integer roomNumber) {
    // Verify the room exists
    roomRepo.findByNumber(roomNumber)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

    // Determine the floor (e.g., room 203 -> floor 2)
    int floor = roomNumber / 100;

    // Get all rooms on this floor
    List<Room> floorRooms = roomRepo.findAll().stream()
        .filter(r -> r.getNumber() != null && r.getNumber() / 100 == floor)
        .sorted((a, b) -> a.getNumber().compareTo(b.getNumber()))
        .toList();

    // For each room, get the most recent invoice data
    List<RoomComparisonDTO> result = new ArrayList<>();

    for (Room room : floorRooms) {
      List<Invoice> roomInvoices = invoiceRepo.findByRoom_Id(room.getId());

      // Get the most recent invoice
      Invoice latestInvoice = roomInvoices.stream()
          .filter(inv -> inv.getBillingYear() != null && inv.getBillingMonth() != null)
          .max((a, b) -> {
            int yearComp = a.getBillingYear().compareTo(b.getBillingYear());
            if (yearComp != 0) return yearComp;
            return a.getBillingMonth().compareTo(b.getBillingMonth());
          })
          .orElse(null);

      // Build the comparison data
      RoomComparisonDTO dto = RoomComparisonDTO.builder()
          .roomNumber(room.getNumber())
          .electricityUnits(latestInvoice != null ? latestInvoice.getElectricityUnits() : BigDecimal.ZERO)
          .electricityBaht(latestInvoice != null ? latestInvoice.getElectricityBaht() : BigDecimal.ZERO)
          .waterUnits(latestInvoice != null ? latestInvoice.getWaterUnits() : BigDecimal.ZERO)
          .waterBaht(latestInvoice != null ? latestInvoice.getWaterBaht() : BigDecimal.ZERO)
          .isSelected(room.getNumber().equals(roomNumber))
          .build();

      result.add(dto);
    }

    return result;
  }

  /**
   * Get monthly trend data for all rooms owned by a specific tenant
   * Returns a list of RoomTrendDTO, each containing monthly trends for one room
   */
  @GetMapping("/tenant-rooms/{tenantId}")
  public List<RoomTrendDTO> getTenantRoomsTrend(
      @PathVariable Long tenantId,
      @RequestParam(defaultValue = "6") Integer months
  ) {
    // Verify tenant exists
    Tenant tenant = tenantRepo.findById(tenantId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));

    // Limit months to reasonable range
    if (months > 24) months = 24;
    if (months < 1) months = 1;

    // Get all active leases for this tenant
    List<Lease> activeLeases = leaseRepo.findByTenantIdWithRefs(tenantId).stream()
        .filter(lease -> lease.getStatus() == Lease.Status.ACTIVE)
        .collect(Collectors.toList());

    // Build room trend data for each room
    List<RoomTrendDTO> result = new ArrayList<>();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");

    for (Lease lease : activeLeases) {
      Room room = lease.getRoom();

      // Get all invoices for this room
      List<Invoice> roomInvoices = invoiceRepo.findByRoom_Id(room.getId());

      // Group by year-month
      Map<String, List<Invoice>> byMonth = new HashMap<>();
      for (Invoice inv : roomInvoices) {
        if (inv.getBillingYear() != null && inv.getBillingMonth() != null) {
          String key = String.format("%04d-%02d", inv.getBillingYear(), inv.getBillingMonth());
          byMonth.computeIfAbsent(key, k -> new ArrayList<>()).add(inv);
        }
      }

      // Build monthly trend data for last N months
      List<MonthlyTrendDTO> monthlyTrends = new ArrayList<>();
      YearMonth current = YearMonth.now();

      for (int i = months - 1; i >= 0; i--) {
        YearMonth targetMonth = current.minusMonths(i);
        String key = targetMonth.format(formatter);

        List<Invoice> monthInvoices = byMonth.getOrDefault(key, new ArrayList<>());

        MonthlyTrendDTO trend = MonthlyTrendDTO.builder()
            .month(key)
            .electricityUnits(sumField(monthInvoices, Invoice::getElectricityUnits))
            .electricityBaht(sumField(monthInvoices, Invoice::getElectricityBaht))
            .waterUnits(sumField(monthInvoices, Invoice::getWaterUnits))
            .waterBaht(sumField(monthInvoices, Invoice::getWaterBaht))
            .build();

        monthlyTrends.add(trend);
      }

      // Create RoomTrendDTO for this room
      RoomTrendDTO roomTrend = RoomTrendDTO.builder()
          .roomId(room.getId())
          .roomNumber(room.getNumber())
          .monthlyTrends(monthlyTrends)
          .build();

      result.add(roomTrend);
    }

    // Sort by room number
    result.sort((a, b) -> a.getRoomNumber().compareTo(b.getRoomNumber()));

    return result;
  }

  /**
   * Get room-level comparison data for a specific month (all rooms or filtered by floor)
   * Returns usage data for each room in the specified month, optionally filtered by floor
   */
  @GetMapping("/month-all-rooms/{year}/{month}")
  public List<RoomComparisonDTO> getMonthAllRoomsComparison(
      @PathVariable Integer year,
      @PathVariable Integer month,
      @RequestParam(required = false) Integer floor
  ) {
    // Get all invoices for the specified month
    List<Invoice> invoices = invoiceRepo.findByBillingYearAndBillingMonth(year, month);

    // Filter by floor if specified
    if (floor != null) {
      invoices = invoices.stream()
          .filter(inv -> inv.getRoom() != null &&
                         inv.getRoom().getNumber() != null &&
                         inv.getRoom().getNumber() / 100 == floor)
          .collect(Collectors.toList());
    }

    // Group by room
    Map<Integer, List<Invoice>> byRoom = new HashMap<>();
    for (Invoice inv : invoices) {
      if (inv.getRoom() != null && inv.getRoom().getNumber() != null) {
        Integer roomNumber = inv.getRoom().getNumber();
        byRoom.computeIfAbsent(roomNumber, k -> new ArrayList<>()).add(inv);
      }
    }

    // Build room comparison data
    List<RoomComparisonDTO> result = new ArrayList<>();

    for (Map.Entry<Integer, List<Invoice>> entry : byRoom.entrySet()) {
      Integer roomNumber = entry.getKey();
      List<Invoice> roomInvoices = entry.getValue();

      RoomComparisonDTO dto = RoomComparisonDTO.builder()
          .roomNumber(roomNumber)
          .electricityUnits(sumField(roomInvoices, Invoice::getElectricityUnits))
          .electricityBaht(sumField(roomInvoices, Invoice::getElectricityBaht))
          .waterUnits(sumField(roomInvoices, Invoice::getWaterUnits))
          .waterBaht(sumField(roomInvoices, Invoice::getWaterBaht))
          .isSelected(false) // No room is pre-selected in this view
          .build();

      result.add(dto);
    }

    // Sort by room number
    result.sort((a, b) -> a.getRoomNumber().compareTo(b.getRoomNumber()));

    return result;
  }
}
