package com.devsop.project.apartmentinvoice.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable; 
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.devsop.project.apartmentinvoice.entity.Lease;
import com.devsop.project.apartmentinvoice.entity.Lease.Status;
import com.devsop.project.apartmentinvoice.entity.Room;
import com.devsop.project.apartmentinvoice.entity.Tenant;
import com.devsop.project.apartmentinvoice.repository.LeaseRepository;
import com.devsop.project.apartmentinvoice.service.LeaseService;

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
  public List<LeaseView> all(@RequestParam(required = false) Status status) {
    List<Lease> leases = leaseRepo.findAllWithRefs();
    if (status != null) {
      leases = leases.stream().filter(l -> l.getStatus() == status).toList();
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

  @PostMapping
  public LeaseView create(@Valid @RequestBody Lease draft) {
    return toView(leaseService.createLease(draft));
  }

  @PutMapping("/{id}")
  public LeaseView update(@PathVariable Long id, @RequestBody Lease patch) {
    return toView(leaseService.updateLease(id, patch));
  }
  
  @PostMapping("/{id}/end")
  public LeaseView end(@PathVariable Long id,
                       @RequestParam(required = false)
                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
    return toView(leaseService.endLease(id, endDate));
  }
  
  @PostMapping("/{id}/settle")
  public LeaseView settle(@PathVariable Long id,
                          @RequestParam(required = false)
                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    return toView(leaseService.settleLease(id, date));
  }
  
  @DeleteMapping("/{id}")
  public void delete(@PathVariable Long id) {
    leaseRepo.deleteById(id);
  }
}
