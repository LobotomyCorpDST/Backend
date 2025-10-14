package com.devsop.project.apartmentinvoice.service;

import com.devsop.project.apartmentinvoice.dto.tenant.TenantCreateRequest;
import com.devsop.project.apartmentinvoice.dto.tenant.TenantResponse;
import com.devsop.project.apartmentinvoice.entity.Tenant;
import com.devsop.project.apartmentinvoice.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TenantService {

  private final TenantRepository repo;

  @Transactional
  public TenantResponse create(TenantCreateRequest req) {
    // ตัวอย่าง duplicate check แบบเบา ๆ: ใช้ phone หรือ lineId ถ้ามี
    if (req.getPhone() != null && !req.getPhone().isBlank() && repo.existsByPhone(req.getPhone())) {
      throw new DuplicateResourceException("Phone already exists");
    }
    if (req.getLineId() != null && !req.getLineId().isBlank() && repo.existsByLineId(req.getLineId())) {
      throw new DuplicateResourceException("Line ID already exists");
    }

    Tenant t = new Tenant();
    t.setName(req.getName());
    t.setPhone(req.getPhone());
    t.setLineId(req.getLineId());

    t = repo.save(t);
    return TenantResponse.builder()
        .id(t.getId())
        .name(t.getName())
        .phone(t.getPhone())
        .lineId(t.getLineId())
        .build();
  }

  public static class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String msg) { super(msg); }
  }
}
