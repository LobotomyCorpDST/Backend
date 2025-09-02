package com.devsop.project.apartmentinvoice.service;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsop.project.apartmentinvoice.entity.Lease;
import com.devsop.project.apartmentinvoice.entity.Lease.Status;
import com.devsop.project.apartmentinvoice.entity.Room;
import com.devsop.project.apartmentinvoice.entity.Tenant;
import com.devsop.project.apartmentinvoice.repository.LeaseRepository;
import com.devsop.project.apartmentinvoice.repository.RoomRepository;
import com.devsop.project.apartmentinvoice.repository.TenantRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LeaseService {

  private final LeaseRepository leaseRepo;
  private final RoomRepository roomRepo;
  private final TenantRepository tenantRepo;

  @Transactional
  public Lease createLease(Lease draft) {
    if (draft.getRoom() == null || draft.getRoom().getId() == null)
      throw new IllegalArgumentException("room.id is required");
    if (draft.getTenant() == null || draft.getTenant().getId() == null)
      throw new IllegalArgumentException("tenant.id is required");

    Room room = roomRepo.findById(draft.getRoom().getId()).orElseThrow();
    Tenant tenant = tenantRepo.findById(draft.getTenant().getId()).orElseThrow();

    // ถ้าห้องไม่ว่าง (มี lease ACTIVE อยู่) ไม่ให้สร้างซ้อน
    boolean hasActive = leaseRepo.findByRoom_IdAndStatus(room.getId(), Status.ACTIVE).stream().findAny().isPresent();
    if (hasActive) throw new IllegalStateException("Room already has an ACTIVE lease.");

    Lease lease = Lease.builder()
        .room(room)
        .tenant(tenant)
        .startDate(draft.getStartDate() != null ? draft.getStartDate() : LocalDate.now())
        .endDate(draft.getEndDate())
        .monthlyRent(draft.getMonthlyRent())
        .depositBaht(draft.getDepositBaht())
        .status(Status.ACTIVE)
        .notes(draft.getNotes())
        .build();

    // อัปเดตสถานะห้อง
    room.setStatus("OCCUPIED");
    room.setTenant(tenant);

    roomRepo.save(room);
    return leaseRepo.save(lease);
  }

  @Transactional
  public Lease updateLease(Long id, Lease patch) {
    Lease l = leaseRepo.findById(id).orElseThrow();

    if (patch.getStartDate() != null) l.setStartDate(patch.getStartDate());
    if (patch.getEndDate() != null)   l.setEndDate(patch.getEndDate());
    if (patch.getMonthlyRent() != null) l.setMonthlyRent(patch.getMonthlyRent());
    if (patch.getDepositBaht() != null) l.setDepositBaht(patch.getDepositBaht());
    if (patch.getNotes() != null) l.setNotes(patch.getNotes());

    // *ถ้าอยากย้ายผู้เช่าหรือย้ายห้อง* (optional)
    if (patch.getTenant() != null && patch.getTenant().getId() != null) {
      Tenant t = tenantRepo.findById(patch.getTenant().getId()).orElseThrow();
      l.setTenant(t);
      // sync room.tenant ด้วย
      l.getRoom().setTenant(t);
      roomRepo.save(l.getRoom());
    }
    if (patch.getRoom() != null && patch.getRoom().getId() != null) {
      Room r = roomRepo.findById(patch.getRoom().getId()).orElseThrow();
      l.setRoom(r);
    }

    return leaseRepo.save(l);
  }

  @Transactional
  public Lease endLease(Long id, LocalDate endDate) {
    Lease l = leaseRepo.findById(id).orElseThrow();
    l.setStatus(Status.ENDED);
    l.setEndDate(endDate != null ? endDate : LocalDate.now());

    // เคลียร์สถานะห้อง
    Room room = l.getRoom();
    room.setStatus("FREE");
    room.setTenant(null);

    roomRepo.save(room);
    return leaseRepo.save(l);
  }
}
