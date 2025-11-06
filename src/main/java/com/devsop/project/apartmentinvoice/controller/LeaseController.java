package com.devsop.project.apartmentinvoice.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.devsop.project.apartmentinvoice.dto.BulkPrintRequest;
import com.devsop.project.apartmentinvoice.entity.Lease;
import com.devsop.project.apartmentinvoice.entity.Lease.Status;
import com.devsop.project.apartmentinvoice.entity.Room;
import com.devsop.project.apartmentinvoice.entity.Tenant;
import com.devsop.project.apartmentinvoice.repository.LeaseRepository;
import com.devsop.project.apartmentinvoice.repository.RoomRepository;
import com.devsop.project.apartmentinvoice.service.LeaseService;
import com.devsop.project.apartmentinvoice.service.PdfService;

import jakarta.validation.Valid;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RestController
@RequestMapping("/api/leases")
@RequiredArgsConstructor
public class LeaseController {

  private final LeaseRepository leaseRepo;
  private final LeaseService leaseService;
  private final RoomRepository roomRepo;
  private final PdfService pdfService;

  // ---------------------- DTOs ----------------------

  @Getter @Setter @AllArgsConstructor @NoArgsConstructor
  public static class RoomView {
    private Long id;
    private Integer number;
    private String status;
  }

  @Getter @Setter @AllArgsConstructor @NoArgsConstructor
  public static class TenantView {
    private Long id;
    private String name;
    private String phone;
    private String lineId;
  }

  @Getter @Setter @AllArgsConstructor @NoArgsConstructor
  public static class LeaseView {
    private Long id;
    private Status status;
    private LocalDate startDate;
    private LocalDate endDate;

    private BigDecimal monthlyRent;
    private BigDecimal depositBaht;

    private String customName;
    private String customIdCard;
    private String customAddress;
    private String customRules;

    private Boolean settled;
    private LocalDate settledDate;

    private RoomView room;
    private TenantView tenant;
  }

  private static LeaseView toView(Lease l) {
    Room r = l.getRoom();
    Tenant t = l.getTenant();
    RoomView rv = (r == null) ? null : new RoomView(r.getId(), r.getNumber(), r.getStatus());
    TenantView tv = (t == null) ? null : new TenantView(t.getId(), t.getName(), t.getPhone(), t.getLineId());
    LeaseView v = new LeaseView();
    v.setId(l.getId());
    v.setStatus(l.getStatus());
    v.setStartDate(l.getStartDate());
    v.setEndDate(l.getEndDate());
    v.setMonthlyRent(l.getMonthlyRent());
    v.setDepositBaht(l.getDepositBaht());
    v.setCustomName(l.getCustomName());
    v.setCustomIdCard(l.getCustomIdCard());
    v.setCustomAddress(l.getCustomAddress());
    v.setCustomRules(l.getCustomRules());
    v.setSettled(l.getSettled());
    v.setSettledDate(l.getSettledDate());
    v.setRoom(rv);
    v.setTenant(tv);
    return v;
  }

  // ---------------------- Query ----------------------

  @GetMapping
  @Transactional(readOnly = true)
  public List<LeaseView> all(
      @RequestParam(required = false) Status status,
      @RequestParam(required = false) String search) {
    List<Lease> leases = leaseRepo.findAllWithRefs();

    // Filter by status if provided
    if (status != null) {
      leases = leases.stream().filter(l -> l.getStatus() == status).toList();
    }

    // Filter by search term if provided
    if (search != null && !search.trim().isEmpty()) {
      String searchLower = search.toLowerCase();
      leases = leases.stream()
          .filter(l ->
              l.getId().toString().contains(search) ||
              (l.getRoom() != null && l.getRoom().getNumber().toString().contains(search)) ||
              (l.getTenant() != null && l.getTenant().getName() != null &&
                  l.getTenant().getName().toLowerCase().contains(searchLower))
          )
          .toList();
    }

    return leases.stream().map(LeaseController::toView).toList();
  }

  @GetMapping("/{id}")
  @Transactional(readOnly = true)
  public LeaseView get(@PathVariable Long id) {
    Lease l = leaseRepo.findByIdWithRefs(id).orElseThrow();
    return toView(l);
  }

  @GetMapping("/by-room/{roomId}")
  @Transactional(readOnly = true)
  public List<LeaseView> byRoom(@PathVariable Long roomId,
                                @RequestParam(required = false) Boolean activeOnly) {
    List<Lease> leases = leaseRepo.findByRoomIdWithRefs(roomId);
    if (Boolean.TRUE.equals(activeOnly)) {
      leases = leases.stream().filter(l -> l.getStatus() == Status.ACTIVE).toList();
    }
    return leases.stream().map(LeaseController::toView).toList();
  }

  @GetMapping("/active")
  @Transactional(readOnly = true)
  public ResponseEntity<LeaseView> getActiveByRoomNumber(@RequestParam Integer roomNumber) {
    return leaseRepo.findFirstActiveByRoomNumber(roomNumber)
        .map(lease -> ResponseEntity.ok(toView(lease)))
        .orElse(ResponseEntity.noContent().build());
  }

  @GetMapping("/history/{roomNumber}")
  @Transactional(readOnly = true)
  public List<LeaseView> historyByRoomNumber(@PathVariable Integer roomNumber) {
    return leaseRepo.findHistoryByRoomNumberWithRefs(roomNumber)
                    .stream().map(LeaseController::toView).collect(Collectors.toList());
  }

  @GetMapping("/by-tenant/{tenantId}")
  @Transactional(readOnly = true)
  public List<LeaseView> byTenant(@PathVariable Long tenantId) {
    return leaseRepo.findByTenantIdWithRefs(tenantId)
                    .stream().map(LeaseController::toView).toList();
  }

  // ---------------------- Create / Update / End / Settle ----------------------

  /**
   * รองรับ payload หลายรูปแบบ:
   * - legacy: { "tenantId":1, "roomId":10, "startDate":"2025-10-17" }
   * - แบบ nested: { "tenant":{"id":1}, "room":{"id":10} | {"number":101}, "startDate":"..." }
   * - แบบใช้เลขห้อง: { "tenantId":1, "roomNumber":101, "startDate":"..." }
   * พร้อมฟิลด์เสริม: monthlyRent, depositBaht, customName, customIdCard, customAddress, customRules
   */
  @PostMapping
  public ResponseEntity<LeaseView> create(@RequestBody CreateLeaseRequest req) {
    // ----- resolve tenantId -----
    Long tenantId = req.getTenantId();
    if (tenantId == null && req.getTenant() != null) {
      tenantId = req.getTenant().getId();
    }

    // ----- resolve ห้อง: roomId หรือ roomNumber -----
    Long roomId = req.getRoomId();
    Integer roomNumber = null;

    if (roomId == null) {
      // จาก object room
      if (req.getRoom() != null) {
        if (req.getRoom().getId() != null) {
          roomId = req.getRoom().getId();
        } else {
          roomNumber = req.getRoom().getNumber();
        }
      }
      // จาก field ตรง ๆ
      if (roomId == null && roomNumber == null) {
        roomNumber = req.getRoomNumber();
      }
      // แปลง roomNumber -> roomId เผื่อเคสที่ service เวอร์ชันนี้ต้องการ id
      if (roomId == null && roomNumber != null) {
        Room r = roomRepo.findByNumber(roomNumber)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Room number not found"));
        roomId = r.getId();
      }
    }

    if (tenantId == null || roomId == null || req.getStartDate() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "tenantId, roomId/roomNumber and startDate are required");
    }

    // ---- ประกอบ draft เพื่อให้เก็บฟิลด์เสริมตั้งแต่แรก
    Tenant t = new Tenant(); t.setId(tenantId);
    Room r = new Room();
    r.setId(roomId);                  // ใช้ id ที่ resolve แล้ว
    if (roomNumber != null) r.setNumber(roomNumber); // แนบ number ถ้ามี (ไม่บังคับ)

    Lease draft = Lease.builder()
        .tenant(t)
        .room(r)
        .startDate(req.getStartDate())
        .endDate(req.getEndDate())
        .monthlyRent(req.getMonthlyRent())
        .depositBaht(req.getDepositBaht())
        .customName(req.getCustomName())
        .customIdCard(req.getCustomIdCard())
        .customAddress(req.getCustomAddress())
        .customRules(req.getCustomRules())
        .build();

    Lease lease = leaseService.createLease(draft);
    return ResponseEntity.status(201).body(toView(lease));
  }

  @Getter @Setter @NoArgsConstructor @AllArgsConstructor
  public static class CreateLeaseRequest {
    // แบบ legacy
    private Long tenantId;
    private Long roomId;
    // แบบใช้เลขห้องตรง ๆ
    private Integer roomNumber;
    // แบบ nested
    private Ref room;
    private Ref tenant;

    private LocalDate startDate;
    private LocalDate endDate;

    // ฟิลด์เสริม
    private BigDecimal monthlyRent;
    private BigDecimal depositBaht;
    private String customName;
    private String customIdCard;
    private String customAddress;
    private String customRules;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class Ref {
      private Long id;
      private Integer number; // สำหรับ room เท่านั้น
    }
  }

  @PutMapping("/{id}")
  public LeaseView update(@PathVariable Long id, @RequestBody Lease patch) {
    return toView(leaseService.updateLease(id, patch));
  }

  @PutMapping("/{id}/end")
  public ResponseEntity<LeaseView> end(@PathVariable Long id, @RequestBody EndLeaseRequest req) {
    Lease lease = leaseService.endLease(id, req.getEndDate());
    return ResponseEntity.ok(toView(lease));
  }

  @Getter @Setter @NoArgsConstructor @AllArgsConstructor
  public static class EndLeaseRequest {
    private LocalDate endDate;
  }

  @PostMapping("/{id}/settle")
  public LeaseView settle(@PathVariable Long id,
                          @RequestParam(required = false)
                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    return toView(leaseService.settleLease(id, date));
  }

    @DeleteMapping("/{id}")
  public void delete(@PathVariable Long id) {
    leaseService.deleteLease(id);
  }

    @PatchMapping("/{id}")
  public LeaseView patch(@PathVariable Long id, @RequestBody Lease patch) {
    return toView(leaseService.updateLease(id, patch));
  }

  // ---------- Bulk PDF Generator ----------
  @PostMapping(value = "/bulk-pdf", produces = MediaType.APPLICATION_PDF_VALUE)
  public ResponseEntity<byte[]> getBulkLeasePdf(@Valid @RequestBody BulkPrintRequest request) {
    List<byte[]> pdfList = new java.util.ArrayList<>();

    for (Long id : request.getIds()) {
      try {
        Lease lease = leaseRepo.findByIdWithRefs(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lease not found: " + id));

        byte[] pdf = pdfService.generateLeasePdf(lease);
        pdfList.add(pdf);
      } catch (Exception e) {
        // Log error but continue with other leases
        System.err.println("Failed to generate PDF for lease " + id + ": " + e.getMessage());
      }
    }

    if (pdfList.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No valid leases found to print");
    }

    byte[] mergedPdf = pdfService.mergePdfs(pdfList);
    String filename = "leases-bulk-" + java.time.LocalDateTime.now().format(
        java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".pdf";

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + filename)
        .contentType(MediaType.APPLICATION_PDF)
        .body(mergedPdf);
  }
}
