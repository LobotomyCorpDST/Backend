package com.devsop.project.apartmentinvoice.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import com.devsop.project.apartmentinvoice.dto.CreateInvoiceRequest;
import com.devsop.project.apartmentinvoice.entity.Invoice;
import com.devsop.project.apartmentinvoice.entity.Lease;
import com.devsop.project.apartmentinvoice.entity.Maintenance;
import com.devsop.project.apartmentinvoice.entity.Maintenance.Status;
import com.devsop.project.apartmentinvoice.entity.Room;
import com.devsop.project.apartmentinvoice.entity.Tenant;
import com.devsop.project.apartmentinvoice.repository.InvoiceRepository;
import com.devsop.project.apartmentinvoice.repository.LeaseRepository;
import com.devsop.project.apartmentinvoice.repository.MaintenanceRepository;
import com.devsop.project.apartmentinvoice.repository.RoomRepository;
import com.devsop.project.apartmentinvoice.service.PdfService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

  private final InvoiceRepository repo;
  private final RoomRepository roomRepo;
  private final LeaseRepository leaseRepo;
  private final MaintenanceRepository maintenanceRepo;
  private final PdfService pdfService;

  // ---------- JSON APIs ----------

  @ResponseBody
  @GetMapping
  public List<Invoice> all() {
    return repo.findAll();
  }

  /** ดึงใบแจ้งหนี้รายใบ (สำหรับหน้า detail) */
  @ResponseBody
  @GetMapping("/{id}")
  public Invoice getOne(@PathVariable Long id) {
    return repo.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));
  }

  @ResponseBody
  @GetMapping("/by-room/{roomId}")
  public List<Invoice> byRoom(@PathVariable Long roomId) {
    return repo.findByRoom_Id(roomId);
  }

  @ResponseBody
  @GetMapping("/by-tenant/{tenantId}")
  public List<Invoice> byTenant(@PathVariable Long tenantId) {
    return repo.findByTenant_Id(tenantId);
  }

  @ResponseBody
  @GetMapping("/month/{year}/{month}")
  public List<Invoice> byMonth(@PathVariable Integer year, @PathVariable Integer month) {
    return repo.findByBillingYearAndBillingMonth(year, month);
  }

  /** ประวัติใบแจ้งหนี้ของห้อง (ไว้ให้หน้า Room Detail) */
  @ResponseBody
  @GetMapping("/history/by-room/{roomId}")
  public List<Invoice> historyByRoom(@PathVariable Long roomId) {
    return repo.findByRoom_Id(roomId);
  }

  /**
   * สร้างใบแจ้งหนี้
   */
  @ResponseBody
  @PostMapping
  public Invoice create(
      @Valid @RequestBody CreateInvoiceRequest req,
      @RequestParam(name = "includeCommonFee", defaultValue = "false") boolean includeCommonFee,
      @RequestParam(name = "includeGarbageFee", defaultValue = "false") boolean includeGarbageFee
  ) {
    // ===== วันที่พื้นฐาน =====
    LocalDate issueDate = (req.getIssueDate() != null) ? req.getIssueDate() : LocalDate.now();
    LocalDate dueDate   = (req.getDueDate()   != null) ? req.getDueDate()   : issueDate.plusDays(7);
    Integer  year       = (req.getBillingYear()  != null) ? req.getBillingYear()  : issueDate.getYear();
    Integer  month      = (req.getBillingMonth() != null) ? req.getBillingMonth() : issueDate.getMonthValue();

    // ===== ห้อง (ต้องมี roomId) =====
    Room room = roomRepo.findById(req.getRoomId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

    // ===== Lease ที่ ACTIVE ณ วัน issueDate =====
    Lease lease = leaseRepo.findActiveLeaseByRoomOnDate(room.getId(), issueDate).orElse(null);

    // ===== เลือก tenant ตามกติกา =====
    Tenant tenantFromLease = (lease != null) ? lease.getTenant() : null;
    Tenant tenant;
    if (req.getTenantId() == null) {
      tenant = tenantFromLease;
      if (tenant == null) {
        throw new IllegalArgumentException("No active lease found and tenantId not provided.");
      }
    } else {
      if (tenantFromLease != null && !req.getTenantId().equals(tenantFromLease.getId())) {
        throw new IllegalArgumentException("Tenant does not match active lease for the room/date.");
      }
      tenant = new Tenant();
      tenant.setId(req.getTenantId());
    }

    // ===== ประกอบ Invoice =====
    Invoice in = new Invoice();
    in.setRoom(room);
    in.setTenant(tenant);
    in.setIssueDate(issueDate);
    in.setDueDate(dueDate);
    in.setBillingYear(year);
    in.setBillingMonth(month);

    // ค่าเช่า
    BigDecimal rent = req.getRentBaht();
    if (rent == null && lease != null) rent = lease.getMonthlyRent();
    if (rent == null) rent = BigDecimal.ZERO;
    in.setRentBaht(rent);

    // ไฟฟ้า
    BigDecimal elecBaht = req.getElectricityBaht();
    if (elecBaht == null && req.getElectricityUnits() != null && req.getElectricityRate() != null) {
      elecBaht = req.getElectricityUnits().multiply(req.getElectricityRate());
    }
    in.setElectricityUnits(req.getElectricityUnits());
    in.setElectricityRate(req.getElectricityRate());
    in.setElectricityBaht(elecBaht);

    // น้ำ
    BigDecimal waterBaht = req.getWaterBaht();
    if (waterBaht == null && req.getWaterUnits() != null && req.getWaterRate() != null) {
      waterBaht = req.getWaterUnits().multiply(req.getWaterRate());
    }
    in.setWaterUnits(req.getWaterUnits());
    in.setWaterRate(req.getWaterRate());
    in.setWaterBaht(waterBaht);

    // อื่น ๆ
    in.setOtherBaht(req.getOtherBaht() != null ? req.getOtherBaht() : BigDecimal.ZERO);

    // ค่าส่วนกลาง/ค่าขยะ
    BigDecimal commonFee  = req.getCommonFeeBaht();
    BigDecimal garbageFee = req.getGarbageFeeBaht();
    if (commonFee == null && includeCommonFee)   commonFee  = room.getCommonFeeBaht();
    if (garbageFee == null && includeGarbageFee) garbageFee = room.getGarbageFeeBaht();
    if (commonFee  == null) commonFee  = BigDecimal.ZERO;
    if (garbageFee == null) garbageFee = BigDecimal.ZERO;
    in.setCommonFeeBaht(commonFee);
    in.setGarbageFeeBaht(garbageFee);

    // ===== รวม Maintenance ของเดือนบิลนี้ =====
    LocalDate firstDay = LocalDate.of(year, month, 1);
    LocalDate lastDay  = firstDay.withDayOfMonth(firstDay.lengthOfMonth());

    List<Maintenance> items = maintenanceRepo
        .findByRoom_IdAndStatusAndCompletedDateBetween(room.getId(), Status.COMPLETED, firstDay, lastDay);

    BigDecimal maintenanceSum = items.stream()
        .map(Maintenance::getCostBaht)
        .filter(c -> c != null)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    in.setMaintenanceBaht(maintenanceSum);

    // รวมยอด
    BigDecimal total = BigDecimal.ZERO;
    total = total.add(sum(in.getRentBaht()));
    total = total.add(sum(in.getElectricityBaht()));
    total = total.add(sum(in.getWaterBaht()));
    total = total.add(sum(in.getOtherBaht()));
    total = total.add(sum(in.getCommonFeeBaht()));
    total = total.add(sum(in.getGarbageFeeBaht()));
    total = total.add(sum(in.getMaintenanceBaht()));
    in.setTotalBaht(total);

    return repo.save(in);
  }

  // ---------- Mark as PAID / UNPAID ----------

  @ResponseBody
  @PostMapping("/{id}/mark-paid")
  public Invoice markPaid(
      @PathVariable Long id,
      @RequestParam("paidDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate paidDate
  ) {
    Invoice inv = repo.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));
    inv.setStatus(Invoice.Status.PAID);
    inv.setPaidDate(paidDate);
    return repo.save(inv);
  }

  @ResponseBody
  @PatchMapping("/{id}/unpaid")
  public Invoice markUnpaid(@PathVariable Long id) {
    Invoice inv = repo.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));
    inv.setStatus(Invoice.Status.PENDING);
    inv.setPaidDate(null);
    return repo.save(inv);
  }

  // ---------- View / PDF ----------

  @GetMapping("/{id}/print")
  public String print(@PathVariable Long id, Model model) {
    Invoice invoice = repo.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));
    model.addAttribute("invoice", invoice);
    return "invoice";
  }

  @GetMapping("/{id}/pdf")
  public ResponseEntity<byte[]> exportPdf(@PathVariable Long id) {
    Invoice invoice = repo.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));
    byte[] pdf = pdfService.renderTemplateToPdf("invoice", Map.of("invoice", invoice));

    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=invoice-" + id + ".pdf")
        .body(pdf);
  }

  // ---------- Error handling ----------

  @ResponseBody
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, String>> handleIllegalArg(IllegalArgumentException ex) {
    return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
  }

  // ---------- Helpers ----------

  private static BigDecimal sum(BigDecimal v) {
    return v != null ? v : BigDecimal.ZERO;
  }
}
