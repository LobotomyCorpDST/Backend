package com.devsop.project.apartmentinvoice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.devsop.project.apartmentinvoice.entity.Lease;
import com.devsop.project.apartmentinvoice.repository.LeaseRepository;
import com.devsop.project.apartmentinvoice.service.PdfService;

@RestController
@RequestMapping("/api/leases")
@RequiredArgsConstructor
public class LeasePdfController {

  private final LeaseRepository leaseRepo;
  private final PdfService pdfService;

  @GetMapping(value = "/{id}/print", produces = MediaType.APPLICATION_PDF_VALUE)
  public ResponseEntity<byte[]> printPdf(@PathVariable Long id) {
    Lease lease = leaseRepo.findByIdWithRefs(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lease not found: " + id));

    byte[] pdf = pdfService.generateLeasePdf(lease); // ให้ PdfService สร้าง PDF จากข้อมูล lease

    String filename = "lease-" + id + ".pdf";
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + filename)
        .contentType(MediaType.APPLICATION_PDF)
        .body(pdf);
  }
}
