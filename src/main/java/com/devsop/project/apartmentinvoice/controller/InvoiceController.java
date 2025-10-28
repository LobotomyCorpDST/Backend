package com.devsop.project.apartmentinvoice.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.devsop.project.apartmentinvoice.dto.BulkPrintRequest;
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
import com.devsop.project.apartmentinvoice.service.CsvImportService;
import com.devsop.project.apartmentinvoice.service.InvoiceService;
import com.devsop.project.apartmentinvoice.service.InvoiceSettingsService;
import com.devsop.project.apartmentinvoice.service.PdfService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Paths;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/invoices") // ✅ ใช้สำหรับ REST APIs ทั้งหมด
@RequiredArgsConstructor
public class InvoiceController {

  private final InvoiceRepository repo;
  private final RoomRepository roomRepo;
  private final LeaseRepository leaseRepo;
  private final MaintenanceRepository maintenanceRepo;
  private final PdfService pdfService;
  private final InvoiceService invoiceService;
  private final CsvImportService csvImportService;
  private final InvoiceSettingsService settingsService;

  @Value("${file.upload.dir:./uploads}")
  private String uploadBaseDir;

  // ---------- JSON APIs ----------

  @GetMapping
  public List<Invoice> all() {
    return repo.findAll();
  }

  /** ดึงใบแจ้งหนี้รายใบ (สำหรับหน้า detail) */
  @GetMapping("/{id}")
  public Invoice getOne(@PathVariable Long id) {
    return repo.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));
  }

  @GetMapping("/by-room/{roomId}")
  public List<Invoice> byRoom(@PathVariable Long roomId) {
    return repo.findByRoom_Id(roomId);
  }

  @GetMapping("/by-tenant/{tenantId}")
  public List<Invoice> byTenant(@PathVariable Long tenantId) {
    return repo.findByTenant_Id(tenantId);
  }

  @GetMapping("/month/{year}/{month}")
  public List<Invoice> byMonth(@PathVariable Integer year, @PathVariable Integer month) {
    return repo.findByBillingYearAndBillingMonth(year, month);
  }

  /** ประวัติใบแจ้งหนี้ของห้อง (ไว้ให้หน้า Room Detail) */
  @GetMapping("/history/by-room/{roomId}")
  public List<Invoice> historyByRoom(@PathVariable Long roomId) {
    return repo.findByRoom_Id(roomId);
  }

  // ---------- สร้างใบแจ้งหนี้ ----------

  @PostMapping
  public Invoice create(
      @Valid @RequestBody CreateInvoiceRequest req,
      @RequestParam(name = "includeCommonFee", defaultValue = "false") boolean includeCommonFee,
      @RequestParam(name = "includeGarbageFee", defaultValue = "false") boolean includeGarbageFee
  ) {
    LocalDate issueDate = (req.getIssueDate() != null) ? req.getIssueDate() : LocalDate.now();
    LocalDate dueDate   = (req.getDueDate()   != null) ? req.getDueDate()   : issueDate.plusDays(7);
    Integer  year       = (req.getBillingYear()  != null) ? req.getBillingYear()  : issueDate.getYear();
    Integer  month      = (req.getBillingMonth() != null) ? req.getBillingMonth() : issueDate.getMonthValue();

    Room room = roomRepo.findById(req.getRoomId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

    Lease lease = leaseRepo.findActiveLeaseByRoomOnDate(room.getId(), issueDate).orElse(null);
    Tenant tenantFromLease = (lease != null) ? lease.getTenant() : null;
    Tenant tenant;
    if (req.getTenantId() == null) {
      tenant = tenantFromLease;
      if (tenant == null) throw new IllegalArgumentException("No active lease found and tenantId not provided.");
    } else {
      if (tenantFromLease != null && !req.getTenantId().equals(tenantFromLease.getId())) {
        throw new IllegalArgumentException("Tenant does not match active lease for the room/date.");
      }
      tenant = new Tenant();
      tenant.setId(req.getTenantId());
    }

    Invoice in = new Invoice();
    in.setRoom(room);
    in.setTenant(tenant);
    in.setIssueDate(issueDate);
    in.setDueDate(dueDate);
    in.setBillingYear(year);
    in.setBillingMonth(month);

    // ===== คำนวณค่าใช้จ่าย =====
    var rent = req.getRentBaht();
    if (rent == null && lease != null) rent = lease.getMonthlyRent();
    if (rent == null) rent = java.math.BigDecimal.ZERO;
    in.setRentBaht(rent);

    var elecBaht = req.getElectricityBaht();
    if (elecBaht == null && req.getElectricityUnits() != null && req.getElectricityRate() != null)
      elecBaht = req.getElectricityUnits().multiply(req.getElectricityRate());
    in.setElectricityUnits(req.getElectricityUnits());
    in.setElectricityRate(req.getElectricityRate());
    in.setElectricityBaht(elecBaht);

    var waterBaht = req.getWaterBaht();
    if (waterBaht == null && req.getWaterUnits() != null && req.getWaterRate() != null)
      waterBaht = req.getWaterUnits().multiply(req.getWaterRate());
    in.setWaterUnits(req.getWaterUnits());
    in.setWaterRate(req.getWaterRate());
    in.setWaterBaht(waterBaht);

    in.setOtherBaht(req.getOtherBaht() != null ? req.getOtherBaht() : java.math.BigDecimal.ZERO);

    var commonFee  = req.getCommonFeeBaht();
    var garbageFee = req.getGarbageFeeBaht();
    if (commonFee == null && includeCommonFee)   commonFee  = room.getCommonFeeBaht();
    if (garbageFee == null && includeGarbageFee) garbageFee = room.getGarbageFeeBaht();
    if (commonFee == null)  commonFee  = java.math.BigDecimal.ZERO;
    if (garbageFee == null) garbageFee = java.math.BigDecimal.ZERO;
    in.setCommonFeeBaht(commonFee);
    in.setGarbageFeeBaht(garbageFee);

    // ===== รวม Maintenance ของเดือนบิลนี้ =====
    LocalDate firstDay = LocalDate.of(year, month, 1);
    LocalDate lastDay  = firstDay.withDayOfMonth(firstDay.lengthOfMonth());

    List<Maintenance> items = maintenanceRepo
        .findByRoom_IdAndStatusAndCompletedDateBetween(room.getId(), Status.COMPLETED, firstDay, lastDay);

    var maintenanceSum = items.stream()
        .map(Maintenance::getCostBaht)
        .filter(c -> c != null)
        .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

    in.setMaintenanceBaht(maintenanceSum);

    // รวมยอด
    java.math.BigDecimal total = java.math.BigDecimal.ZERO;
    total = total.add(sum(in.getRentBaht()));
    total = total.add(sum(in.getElectricityBaht()));
    total = total.add(sum(in.getWaterBaht()));
    total = total.add(sum(in.getOtherBaht()));
    total = total.add(sum(in.getCommonFeeBaht()));
    total = total.add(sum(in.getGarbageFeeBaht()));
    total = total.add(sum(in.getMaintenanceBaht()));
    in.setTotalBaht(total);

    // ===== Calculate Accumulated Debt =====
    InvoiceService.DebtCalculation debt = invoiceService.calculateAccumulatedDebt(room.getId(), year, month);
    in.setPreviousBalance(debt.getPreviousBalance());
    in.setInterestCharge(debt.getInterestCharge());
    in.setAccumulatedTotal(total.add(debt.getPreviousBalance()).add(debt.getInterestCharge()));

    return repo.save(in);
  }

  // ---------- PDF Generator ----------
  @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
  public ResponseEntity<byte[]> getInvoicePdf(@PathVariable Long id) {
    Invoice invoice = repo.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));

    // Get invoice settings for payment info and QR code
    com.devsop.project.apartmentinvoice.entity.InvoiceSettings settings = settingsService.getSettings();

    // Build full path to QR code for PDF embedding
    String qrCodeFullPath = null;
    if (settings.getQrCodeImagePath() != null) {
      qrCodeFullPath = java.nio.file.Paths.get(uploadBaseDir, settings.getQrCodeImagePath())
        .toAbsolutePath().toString().replace("\\", "/");
    }

    Map<String, Object> model = new java.util.HashMap<>();
    model.put("invoice", invoice);
    model.put("settings", settings);
    model.put("qrCodeFullPath", qrCodeFullPath);

    byte[] pdf = pdfService.renderTemplateToPdf("invoice", model);

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=invoice-" + id + ".pdf")
        .contentType(MediaType.APPLICATION_PDF)
        .body(pdf);
  }

  // ---------- CSV Import ----------
  @PostMapping("/import-csv")
  public ResponseEntity<?> importCsv(@RequestParam("file") MultipartFile file) {
    CsvImportService.ImportResult result = csvImportService.importInvoicesFromCsv(file);

    return ResponseEntity.ok(Map.of(
      "message", "CSV import completed",
      "successCount", result.getSuccessCount(),
      "failureCount", result.getFailureCount(),
      "totalProcessed", result.getTotalProcessed(),
      "errors", result.getErrors()
    ));
  }

  // ---------- Get Current Month Invoices ----------
  @GetMapping("/current-month")
  public List<Invoice> getCurrentMonthInvoices() {
    return invoiceService.getInvoicesForCurrentMonth();
  }

  // ---------- Bulk PDF Generator ----------
  @PostMapping(value = "/bulk-pdf", produces = MediaType.APPLICATION_PDF_VALUE)
  public ResponseEntity<byte[]> getBulkInvoicePdf(@Valid @RequestBody BulkPrintRequest request) {
    List<byte[]> pdfList = new java.util.ArrayList<>();

    // Get invoice settings once for all PDFs
    com.devsop.project.apartmentinvoice.entity.InvoiceSettings settings = settingsService.getSettings();
    String qrCodeFullPath = null;
    if (settings.getQrCodeImagePath() != null) {
      qrCodeFullPath = java.nio.file.Paths.get(uploadBaseDir, settings.getQrCodeImagePath())
        .toAbsolutePath().toString().replace("\\", "/");
    }

    for (Long id : request.getIds()) {
      try {
        Invoice invoice = repo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found: " + id));

        Map<String, Object> model = new java.util.HashMap<>();
        model.put("invoice", invoice);
        model.put("settings", settings);
        model.put("qrCodeFullPath", qrCodeFullPath);

        byte[] pdf = pdfService.renderTemplateToPdf("invoice", model);
        pdfList.add(pdf);
      } catch (Exception e) {
        // Log error but continue with other invoices
        System.err.println("Failed to generate PDF for invoice " + id + ": " + e.getMessage());
      }
    }

    if (pdfList.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No valid invoices found to print");
    }

    byte[] mergedPdf = pdfService.mergePdfs(pdfList);
    String filename = "invoices-bulk-" + java.time.LocalDateTime.now().format(
        java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".pdf";

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + filename)
        .contentType(MediaType.APPLICATION_PDF)
        .body(mergedPdf);
  }

  // ✅ รองรับลิงก์เดิม /api/invoices/{id}/print (redirect ไป /api/.../pdf ให้ถูก path)
  @GetMapping("/{id}/print")
  public ResponseEntity<Void> redirectPrint(@PathVariable Long id) {
    return ResponseEntity.status(HttpStatus.FOUND) // 302
        .header(HttpHeaders.LOCATION, "/api/invoices/" + id + "/pdf")
        .build();
  }

  // ---------- Mark as PAID / UNPAID ----------

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

  @PatchMapping("/{id}/unpaid")
  public Invoice markUnpaid(@PathVariable Long id) {
    Invoice inv = repo.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));
    inv.setStatus(Invoice.Status.PENDING);
    inv.setPaidDate(null);
    return repo.save(inv);
  }


    // ---------- Edit & Delete ----------

  /** Edit invoice basic fields */
  @PatchMapping("/{id}")
public Invoice update(@PathVariable Long id, @RequestBody Invoice patch) {
  Invoice inv = repo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));

  if (patch.getBillingYear() != null) inv.setBillingYear(patch.getBillingYear());
  if (patch.getBillingMonth() != null) inv.setBillingMonth(patch.getBillingMonth());
  if (patch.getIssueDate() != null) inv.setIssueDate(patch.getIssueDate());
  if (patch.getDueDate() != null) inv.setDueDate(patch.getDueDate());

  // ✅ Update core numeric fields
  if (patch.getElectricityUnits() != null) inv.setElectricityUnits(patch.getElectricityUnits());
  if (patch.getElectricityRate() != null) inv.setElectricityRate(patch.getElectricityRate());
  if (patch.getWaterUnits() != null) inv.setWaterUnits(patch.getWaterUnits());
  if (patch.getWaterRate() != null) inv.setWaterRate(patch.getWaterRate());
  if (patch.getOtherBaht() != null) inv.setOtherBaht(patch.getOtherBaht());
  if (patch.getRentBaht() != null) inv.setRentBaht(patch.getRentBaht());
  if (patch.getCommonFeeBaht() != null) inv.setCommonFeeBaht(patch.getCommonFeeBaht());
  if (patch.getGarbageFeeBaht() != null) inv.setGarbageFeeBaht(patch.getGarbageFeeBaht());
  if (patch.getMaintenanceBaht() != null) inv.setMaintenanceBaht(patch.getMaintenanceBaht());

  // ✅ Auto-recalculate dependent amounts
  if (inv.getElectricityUnits() != null && inv.getElectricityRate() != null) {
    inv.setElectricityBaht(inv.getElectricityUnits().multiply(inv.getElectricityRate()));
  }
  if (inv.getWaterUnits() != null && inv.getWaterRate() != null) {
    inv.setWaterBaht(inv.getWaterUnits().multiply(inv.getWaterRate()));
  }

  // ✅ Always recompute total
  java.math.BigDecimal total = java.math.BigDecimal.ZERO;
  total = total.add(sum(inv.getRentBaht()));
  total = total.add(sum(inv.getElectricityBaht()));
  total = total.add(sum(inv.getWaterBaht()));
  total = total.add(sum(inv.getOtherBaht()));
  total = total.add(sum(inv.getCommonFeeBaht()));
  total = total.add(sum(inv.getGarbageFeeBaht()));
  total = total.add(sum(inv.getMaintenanceBaht()));
  inv.setTotalBaht(total);

  return repo.save(inv);
}


  /** Delete invoice (with FK protection) */
  @org.springframework.web.bind.annotation.DeleteMapping("/{id}")
  public void delete(@PathVariable Long id) {
    Invoice inv = repo.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));

    try {
      repo.delete(inv);
    } catch (org.springframework.dao.DataIntegrityViolationException e) {
      // ✅ Provide a clear human-readable error message for FK constraint
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Cannot delete invoice because it is linked to another record (e.g., payment or maintenance)."
      );
    }
  }

  // ---------- Error handling ----------

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, String>> handleIllegalArg(IllegalArgumentException ex) {
    return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
  }

  // ---------- Helpers ----------
  private static java.math.BigDecimal sum(java.math.BigDecimal v) {
    return v != null ? v : java.math.BigDecimal.ZERO;
  }
}
