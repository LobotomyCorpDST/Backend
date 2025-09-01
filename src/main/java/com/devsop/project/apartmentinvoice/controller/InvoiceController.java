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
import org.springframework.web.bind.annotation.RestController;

import com.devsop.project.apartmentinvoice.entity.Invoice;
import com.devsop.project.apartmentinvoice.repository.InvoiceRepository;
import com.devsop.project.apartmentinvoice.service.PdfService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

  private final InvoiceRepository repo;
  private final PdfService pdfService;

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

  @PostMapping
  public Invoice create(@Valid @RequestBody Invoice in) {

    if (in.getIssueDate() == null) in.setIssueDate(LocalDate.now());
    if (in.getDueDate() == null)   in.setDueDate(in.getIssueDate().plusDays(7));

    if (in.getElectricityBaht() == null && in.getElectricityUnits() != null && in.getElectricityRate() != null) {
      in.setElectricityBaht(in.getElectricityUnits().multiply(in.getElectricityRate()));
    }
    if (in.getWaterBaht() == null && in.getWaterUnits() != null && in.getWaterRate() != null) {
      in.setWaterBaht(in.getWaterUnits().multiply(in.getWaterRate()));
    }
    if (in.getOtherBaht() == null) in.setOtherBaht(BigDecimal.ZERO);
    if (in.getRentBaht()  == null) in.setRentBaht(BigDecimal.ZERO);

    if (in.getTotalBaht() == null) {
      BigDecimal total = BigDecimal.ZERO;
      if (in.getRentBaht() != null) total = total.add(in.getRentBaht());
      if (in.getElectricityBaht() != null) total = total.add(in.getElectricityBaht());
      if (in.getWaterBaht() != null) total = total.add(in.getWaterBaht());
      if (in.getOtherBaht() != null) total = total.add(in.getOtherBaht());
      in.setTotalBaht(total);
    }

    return repo.save(in);
  }

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
