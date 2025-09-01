package com.devsop.project.apartmentinvoice.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.devsop.project.apartmentinvoice.entity.Invoice;
import com.devsop.project.apartmentinvoice.entity.Room;
import com.devsop.project.apartmentinvoice.repository.InvoiceRepository;
import com.devsop.project.apartmentinvoice.repository.RoomRepository;
import com.devsop.project.apartmentinvoice.service.PdfService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

  private final InvoiceRepository repo;
  private final RoomRepository roomRepo;
  private final PdfService pdfService;

  // -------- Queries --------

  @GetMapping
  public List<Invoice> all() {
    return repo.findAll();
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

  /**
   * สร้างใบแจ้งหนี้
   * - เลือกคิดค่าส่วนกลาง/ค่าขยะ ด้วย query params: includeCommonFee / includeGarbageFee (default=false)
   * - ถ้า body ส่ง commonFeeBaht/garbageFeeBaht มาแล้ว จะใช้ค่าที่ส่งมาเป็นอันดับแรก
   * - ถ้ายังเป็น null แต่ include=true จะดึงค่า default จาก Room
   * - ถ้ายังไม่ระบุ issueDate/billingYear/billingMonth จะเติมให้ตามวันที่ปัจจุบัน
   */
  @PostMapping
  public Invoice create(
      @Valid @RequestBody Invoice in,
      @RequestParam(name = "includeCommonFee", defaultValue = "false") boolean includeCommonFee,
      @RequestParam(name = "includeGarbageFee", defaultValue = "false") boolean includeGarbageFee
  ) {

    // วันออกใบแจ้งหนี้ & ครบกำหนด
    if (in.getIssueDate() == null) in.setIssueDate(LocalDate.now());
    if (in.getDueDate() == null)   in.setDueDate(in.getIssueDate().plusDays(7));

    // ตั้งค่าเดือน/ปี ถ้าไม่ได้ส่งมา
    if (in.getBillingYear() == null)  in.setBillingYear(in.getIssueDate().getYear());
    if (in.getBillingMonth() == null) in.setBillingMonth(in.getIssueDate().getMonthValue());

    // โหลด Room จาก DB เพื่อใช้ค่า default และป้องกัน room จาก JSON ที่ไม่ผูก persistence
    if (in.getRoom() == null || in.getRoom().getId() == null) {
      throw new IllegalArgumentException("room.id is required");
    }
    Room room = roomRepo.findById(in.getRoom().getId()).orElseThrow();
    in.setRoom(room);

    // คำนวณไฟฟ้า/น้ำ หากยังไม่ได้คำนวณ
    if (in.getElectricityBaht() == null && in.getElectricityUnits() != null && in.getElectricityRate() != null) {
      in.setElectricityBaht(in.getElectricityUnits().multiply(in.getElectricityRate()));
    }
    if (in.getWaterBaht() == null && in.getWaterUnits() != null && in.getWaterRate() != null) {
      in.setWaterBaht(in.getWaterUnits().multiply(in.getWaterRate()));
    }

    // default 0 สำหรับฟิลด์ยอดต่าง ๆ ที่อาจเป็น null
    if (in.getRentBaht() == null)  in.setRentBaht(BigDecimal.ZERO);
    if (in.getOtherBaht() == null) in.setOtherBaht(BigDecimal.ZERO);

    // ค่าส่วนกลาง/ค่าขยะ: ถ้า client ไม่ส่งมา และ include=true >> ใช้ค่า default จากห้อง
    if (in.getCommonFeeBaht() == null && includeCommonFee) {
      in.setCommonFeeBaht(room.getCommonFeeBaht()); // อาจเป็น null ถ้าไม่ตั้งในห้อง
    }
    if (in.getGarbageFeeBaht() == null && includeGarbageFee) {
      in.setGarbageFeeBaht(room.getGarbageFeeBaht());
    }

    // ถ้ายัง null อยู่ ให้เป็น 0 เพื่อคำนวณ total ง่าย
    if (in.getCommonFeeBaht() == null)  in.setCommonFeeBaht(BigDecimal.ZERO);
    if (in.getGarbageFeeBaht() == null) in.setGarbageFeeBaht(BigDecimal.ZERO);

    // รวมยอดทั้งหมด
    BigDecimal total = BigDecimal.ZERO;
    if (in.getRentBaht()        != null) total = total.add(in.getRentBaht());
    if (in.getElectricityBaht() != null) total = total.add(in.getElectricityBaht());
    if (in.getWaterBaht()       != null) total = total.add(in.getWaterBaht());
    if (in.getOtherBaht()       != null) total = total.add(in.getOtherBaht());
    if (in.getCommonFeeBaht()   != null) total = total.add(in.getCommonFeeBaht());
    if (in.getGarbageFeeBaht()  != null) total = total.add(in.getGarbageFeeBaht());

    in.setTotalBaht(total);

    return repo.save(in);
  }

  // -------- Render views --------

  @GetMapping("/{id}/print")
  public String print(@PathVariable Long id, Model model) {
    Invoice invoice = repo.findById(id).orElseThrow();
    model.addAttribute("invoice", invoice);
    return "invoice";
  }

  @GetMapping("/{id}/pdf")
  public ResponseEntity<byte[]> exportPdf(@PathVariable Long id) {
    Invoice invoice = repo.findById(id).orElseThrow();
    byte[] pdf = pdfService.renderTemplateToPdf("invoice", Map.of("invoice", invoice));

    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=invoice-" + id + ".pdf")
        .body(pdf);
  }
}
