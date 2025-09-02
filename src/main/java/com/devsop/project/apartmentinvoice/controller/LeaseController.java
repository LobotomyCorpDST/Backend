package com.devsop.project.apartmentinvoice.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.format.annotation.DateTimeFormat;
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
import com.devsop.project.apartmentinvoice.repository.LeaseRepository;
import com.devsop.project.apartmentinvoice.service.LeaseService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/leases")
@RequiredArgsConstructor
public class LeaseController {

  private final LeaseRepository leaseRepo;
  private final LeaseService leaseService;

  // ---------------------- Query ----------------------

  /**
   * ดึงรายการสัญญาทั้งหมด (หรือกรองด้วย status)
   * ใช้ fetch-join เพื่อให้ room/tenant ถูกโหลดมาพร้อมกัน (เลี่ยง Lazy proxy)
   */
  @GetMapping
  @Transactional(readOnly = true)
  public List<Lease> all(@RequestParam(required = false) Status status) {
    List<Lease> leases = leaseRepo.findAllWithRefs();
    if (status != null) {
      return leases.stream()
          .filter(l -> l.getStatus() == status)
          .collect(Collectors.toList());
    }
    return leases;
  }

  /**
   * ดึงรายละเอียดสัญญาเดียว (fetch-join)
   */
  @GetMapping("/{id}")
  @Transactional(readOnly = true)
  public Lease get(@PathVariable Long id) {
    return leaseRepo.findByIdWithRefs(id).orElseThrow();
  }

  /**
   * ดึงสัญญาตามห้อง (option: activeOnly)
   */
  @GetMapping("/by-room/{roomId}")
  @Transactional(readOnly = true)
  public List<Lease> byRoom(@PathVariable Long roomId,
                            @RequestParam(required = false) Boolean activeOnly) {
    List<Lease> leases = leaseRepo.findByRoomIdWithRefs(roomId);
    if (Boolean.TRUE.equals(activeOnly)) {
      return leases.stream()
          .filter(l -> l.getStatus() == Status.ACTIVE)
          .collect(Collectors.toList());
    }
    return leases;
  }

  /**
   * ดึงสัญญาตามผู้เช่า (fetch-join)
   */
  @GetMapping("/by-tenant/{tenantId}")
  @Transactional(readOnly = true)
  public List<Lease> byTenant(@PathVariable Long tenantId) {
    return leaseRepo.findByTenantIdWithRefs(tenantId);
  }

  // ---------------------- Create ----------------------

  @PostMapping
  public Lease create(@Valid @RequestBody Lease draft) {
    return leaseService.createLease(draft);
  }

  // ---------------------- Update ----------------------

  @PutMapping("/{id}")
  public Lease update(@PathVariable Long id, @RequestBody Lease patch) {
    return leaseService.updateLease(id, patch);
  }

  // ---------------------- End Lease ----------------------

  @PostMapping("/{id}/end")
  public Lease end(@PathVariable Long id,
                   @RequestParam(required = false)
                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
    return leaseService.endLease(id, endDate);
  }

  // (Optional) ลบสัญญา - ระวังข้อมูลประวัติ
  @DeleteMapping("/{id}")
  public void delete(@PathVariable Long id) {
    leaseRepo.deleteById(id);
  }
}
