package com.devsop.project.apartmentinvoice.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devsop.project.apartmentinvoice.dto.tenant.TenantCreateRequest;
import com.devsop.project.apartmentinvoice.dto.tenant.TenantResponse;
import com.devsop.project.apartmentinvoice.entity.Tenant;
import com.devsop.project.apartmentinvoice.repository.TenantRepository;
import com.devsop.project.apartmentinvoice.service.TenantService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
public class TenantController {

  private final TenantRepository repo;
  private final TenantService service;

  // ==========================
  // GET: ทั้งหมด (ไม่มี @PreAuthorize)
  // ==========================
  @GetMapping
  public List<TenantResponse> getAllTenants() {
    return repo.findAll().stream()
        .map(t -> TenantResponse.builder()
            .id(t.getId())
            .name(t.getName())
            .phone(t.getPhone())
            .lineId(t.getLineId())
            .build())
        .toList();
  }

  // ==========================
  // GET: ตาม id (ไม่เจอ -> 404) — ไม่มี @PreAuthorize
  // ==========================
  @GetMapping("/{id}")
  public ResponseEntity<TenantResponse> getTenantById(@PathVariable Long id) {
    return ResponseEntity.of(
        repo.findById(id).map(t ->
            TenantResponse.builder()
                .id(t.getId())
                .name(t.getName())
                .phone(t.getPhone())
                .lineId(t.getLineId())
                .build()
        )
    );
  }

  // ==========================
  // POST: สร้าง (ต้อง ADMIN)
  // ==========================
  @PostMapping
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ADMIN')")
  public ResponseEntity<TenantResponse> create(@Valid @RequestBody TenantCreateRequest req) {
    TenantResponse created = service.create(req);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  // ==========================
  // PUT: อัปเดต (ต้อง ADMIN)
  // ==========================
  @PutMapping("/{id}")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ADMIN')")
  public Tenant update(@PathVariable Long id, @Valid @RequestBody Tenant t) {
    if (!repo.existsById(id)) {
      throw new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found");
    }
    t.setId(id);
    return repo.save(t);
  }

  // ==========================
  // DELETE: ลบ (ต้อง ADMIN)
  // ==========================
  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ADMIN')")
  public void delete(@PathVariable Long id) {
    if (!repo.existsById(id)) {
      throw new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found");
    }
    repo.deleteById(id);
  }
}
