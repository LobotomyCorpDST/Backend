package com.devsop.project.apartmentinvoice.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import com.devsop.project.apartmentinvoice.entity.Invoice;
import com.devsop.project.apartmentinvoice.repository.InvoiceRepository;
import com.devsop.project.apartmentinvoice.service.PdfService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/invoices") // ✅ ฝั่ง View/HTML ให้ใช้ /invoices ไม่ใช่ /api
@RequiredArgsConstructor
public class InvoiceViewController {

  private final InvoiceRepository repo;
  private final PdfService pdfService;

  /** หน้า HTML สำหรับพิมพ์/ดูในเบราว์เซอร์ */
  @GetMapping("/{id}/print") // ✅ เดิมเขียนซ้อน /api/{id}/print → ทำให้ path เพี้ยน
  public String print(@PathVariable Long id, Model model) {
    Invoice invoice = repo.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));
    model.addAttribute("invoice", invoice);
    return "invoice";
  }

  /** ดาวน์โหลด/เปิด PDF ของใบแจ้งหนี้ (View route) */
  @GetMapping("/{id}/pdf")
  public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id) {
    Invoice invoice = repo.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));

    Map<String, Object> model = new HashMap<>();
    model.put("invoice", invoice);

    byte[] pdf = pdfService.renderTemplateToPdf("invoice", model);

    String filename = "invoice-room" + invoice.getRoom().getNumber()
        + "-" + invoice.getBillingYear() + "-" + invoice.getBillingMonth() + ".pdf";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_PDF);
    headers.setContentDisposition(ContentDisposition.inline().filename(filename).build());

    return ResponseEntity.ok().headers(headers).body(pdf);
  }
}
