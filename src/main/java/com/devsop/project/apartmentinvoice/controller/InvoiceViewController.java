package com.devsop.project.apartmentinvoice.controller;

import java.util.HashMap;
import java.util.Map;

import com.devsop.project.apartmentinvoice.entity.Invoice;
import com.devsop.project.apartmentinvoice.repository.InvoiceRepository;
import com.devsop.project.apartmentinvoice.service.PdfService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/invoices")
@RequiredArgsConstructor
public class InvoiceViewController {

  private final InvoiceRepository repo;
  private final PdfService pdfService;

  @GetMapping("/{id}/print")
  public String print(@PathVariable Long id, Model model) {
    Invoice invoice = repo.findById(id).orElseThrow();
    model.addAttribute("invoice", invoice);
    return "invoice";
  }

  @GetMapping("/{id}/pdf")
  public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id) {
    Invoice invoice = repo.findById(id).orElseThrow();

    Map<String, Object> model = new HashMap<>();
    model.put("invoice", invoice);

    byte[] pdf = pdfService.renderTemplateToPdf("invoice", model);

    String filename = "invoice-room" + invoice.getRoom().getNumber() +
                      "-" + invoice.getBillingYear() + "-" + invoice.getBillingMonth() + ".pdf";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_PDF);
    headers.setContentDisposition(ContentDisposition.inline().filename(filename).build());

    return ResponseEntity.ok().headers(headers).body(pdf);
  }
}
