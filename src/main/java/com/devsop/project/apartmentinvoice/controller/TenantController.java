package com.devsop.project.apartmentinvoice.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.devsop.project.apartmentinvoice.dto.TenantWithRoomsDTO;
import com.devsop.project.apartmentinvoice.entity.Lease;
import com.devsop.project.apartmentinvoice.entity.Tenant;
import com.devsop.project.apartmentinvoice.repository.LeaseRepository;
import com.devsop.project.apartmentinvoice.repository.TenantRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
public class TenantController {

  private final TenantRepository repo;
  private final LeaseRepository leaseRepo;

  // ---------------------- Query ----------------------

  /**
   * ดึงผู้เช่าทั้งหมด
   */
  @GetMapping
  public List<Tenant> all() {
    return repo.findAll();
  }

  /**
   * ดึงผู้เช่าทั้งหมดพร้อมข้อมูลห้องที่มี active lease - รองรับการค้นหา
   */
  @GetMapping("/with-rooms")
  public List<TenantWithRoomsDTO> allWithRooms(@RequestParam(required = false) String search) {
    List<Tenant> tenants = repo.findAll();

    // Filter by search term if provided
    if (search != null && !search.trim().isEmpty()) {
      String searchLower = search.toLowerCase();
      tenants = tenants.stream()
          .filter(tenant ->
              tenant.getId().toString().contains(search) ||
              tenant.getName().toLowerCase().contains(searchLower) ||
              (tenant.getPhone() != null && tenant.getPhone().contains(search)) ||
              (tenant.getLineId() != null && tenant.getLineId().toLowerCase().contains(searchLower))
          )
          .collect(Collectors.toList());
    }

    return tenants.stream().map(tenant -> {
      List<Lease> activeLeases = leaseRepo.findByTenant_IdAndStatus(
          tenant.getId(),
          Lease.Status.ACTIVE
      );
      List<Integer> roomNumbers = activeLeases.stream()
          .map(lease -> lease.getRoom().getNumber())
          .sorted()
          .collect(Collectors.toList());

      return new TenantWithRoomsDTO(
          tenant.getId(),
          tenant.getName(),
          tenant.getPhone(),
          tenant.getLineId(),
          roomNumbers
      );
    }).collect(Collectors.toList());
  }

  /**
   * ดึงผู้เช่ารายเดียวตาม ID (ใช้ในหน้า Lease เพื่อ auto-fill)
   */
  @GetMapping("/{id}")
  public ResponseEntity<Tenant> getOne(@PathVariable Long id) {
    return repo.findById(id)
        .map(ResponseEntity::ok)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));
  }

  // ---------------------- Create / Update / Delete ----------------------

  /**
   * เพิ่มผู้เช่าใหม่
   */
  @PostMapping
  public ResponseEntity<Tenant> create(@Valid @RequestBody Tenant t) {
    Tenant saved = repo.save(t);
    return ResponseEntity.status(HttpStatus.CREATED).body(saved);
  }

  /**
   * อัปเดตข้อมูลผู้เช่า (เช่น ชื่อ, เบอร์โทร, ที่อยู่ ฯลฯ)
   */
  @PutMapping("/{id}")
  public Tenant update(@PathVariable Long id, @Valid @RequestBody Tenant t) {
    if (!repo.existsById(id)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found");
    }
    t.setId(id);
    return repo.save(t);
  }

  /**
   * ลบผู้เช่า
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    if (!repo.existsById(id)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found");
    }
    repo.deleteById(id);
    return ResponseEntity.noContent().build();
  }
}
