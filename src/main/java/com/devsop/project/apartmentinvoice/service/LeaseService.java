package com.devsop.project.apartmentinvoice.service;

import java.time.LocalDate;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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

  /**
   * Overload สำหรับ Controller ที่รับ tenantId / roomId / startDate
   * - ตรวจ 404: tenant/room ต้องมีจริง
   * - กันซ้ำ 409: ห้องต้องไม่มี ACTIVE lease อยู่แล้ว
   * - จากนั้น delegate ไปยัง createLease(Lease draft) เพื่อคงพฤติกรรมเดิมทั้งหมด
   */
  @Transactional
  public Lease createLease(Long tenantId, Long roomId, LocalDate startDate) {
    // ---- หา Tenant
    Tenant tenant = tenantRepo.findById(tenantId)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Tenant id " + tenantId + " not found"));

    // ---- หา Room ด้วย id
    Room room = roomRepo.findById(roomId)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Room id " + roomId + " not found"));

    // ---- กันซ้อน: มี ACTIVE lease อยู่แล้วหรือไม่
    boolean hasActive = leaseRepo.findByRoom_IdAndStatus(room.getId(), Status.ACTIVE)
        .stream().findAny().isPresent();
    if (hasActive) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Room already has an ACTIVE lease");
    }

    // ---- ประกอบ draft แล้วใช้เมธอดเดิม
    Lease draft = Lease.builder()
        .tenant(tenant)
        .room(room)
        .startDate(startDate != null ? startDate : LocalDate.now())
        .build();

    return createLease(draft);
  }

  /**
   * Overload ใหม่: รับ tenantId + roomNumber + startDate
   * ใช้ตอนที่ฝั่ง FE ส่งเลขห้องมาแทน room.id
   */
  @Transactional
  public Lease createLeaseByRoomNumber(Long tenantId, Integer roomNumber, LocalDate startDate) {
    // ---- หา Tenant
    Tenant tenant = tenantRepo.findById(tenantId)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Tenant id " + tenantId + " not found"));

    // ---- หา Room ด้วยเลขห้อง
    Room room = roomRepo.findByNumber(roomNumber)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Room number " + roomNumber + " not found"));

    // ---- กันซ้อน: มี ACTIVE lease อยู่แล้วหรือไม่
    boolean hasActive = leaseRepo.findByRoom_IdAndStatus(room.getId(), Status.ACTIVE)
        .stream().findAny().isPresent();
    if (hasActive) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Room already has an ACTIVE lease");
    }

    // ---- ประกอบ draft แล้วใช้เมธอดเดิม
    Lease draft = Lease.builder()
        .tenant(tenant)
        .room(room)
        .startDate(startDate != null ? startDate : LocalDate.now())
        .build();

    return createLease(draft);
  }

  /**
   * สร้างสัญญาเช่าใหม่ — รองรับการอ้างห้องด้วย room.id หรือ room.number
   * และรองรับฟิลด์เสริม (monthlyRent, depositBaht, customName ฯลฯ) จาก draft
   */
  @Transactional
  public Lease createLease(Lease draft) {
    // ---- ตรวจสอบ input ขั้นต้น
    if (draft.getRoom() == null || (draft.getRoom().getId() == null && draft.getRoom().getNumber() == null)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "room.id or room.number is required");
    }
    if (draft.getTenant() == null || draft.getTenant().getId() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tenant.id is required");
    }

    // ---- หา Room (priority: number > id)
    Room room;
    if (draft.getRoom().getNumber() != null) {
      room = roomRepo.findByNumber(draft.getRoom().getNumber())
          .orElseThrow(() -> new ResponseStatusException(
              HttpStatus.NOT_FOUND, "Room number " + draft.getRoom().getNumber() + " not found"));
    } else {
      room = roomRepo.findById(draft.getRoom().getId())
          .orElseThrow(() -> new ResponseStatusException(
              HttpStatus.NOT_FOUND, "Room id " + draft.getRoom().getId() + " not found"));
    }

    // ---- หา Tenant
    Tenant tenant = tenantRepo.findById(draft.getTenant().getId())
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Tenant id " + draft.getTenant().getId() + " not found"));

    // ---- กันซ้อน: มี ACTIVE lease อยู่แล้วหรือไม่
    boolean hasActive = leaseRepo.findByRoom_IdAndStatus(room.getId(), Status.ACTIVE)
        .stream().findAny().isPresent();
    if (hasActive) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Room already has an ACTIVE lease");
    }

    // ---- สร้าง Lease
    Lease lease = Lease.builder()
        .room(room)
        .tenant(tenant)
        .startDate(draft.getStartDate() != null ? draft.getStartDate() : LocalDate.now())
        .endDate(draft.getEndDate())
        .monthlyRent(draft.getMonthlyRent())
        .depositBaht(draft.getDepositBaht())
        .status(Status.ACTIVE)
        .notes(draft.getNotes())
        // custom printable fields
        .customName(draft.getCustomName())
        .customIdCard(draft.getCustomIdCard())
        .customAddress(draft.getCustomAddress())
        .customRules(draft.getCustomRules())
        // settled flags
        .settled(Boolean.FALSE)
        .build();

    // ---- อัปเดตสถานะห้อง
    room.setStatus("OCCUPIED");
    room.setTenant(tenant);

    roomRepo.save(room);
    return leaseRepo.save(lease);
  }

  @Transactional
  public Lease updateLease(Long id, Lease patch) {
    Lease l = leaseRepo.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lease id " + id + " not found"));

    if (patch.getStartDate() != null)     l.setStartDate(patch.getStartDate());
    if (patch.getEndDate() != null)       l.setEndDate(patch.getEndDate());
    if (patch.getMonthlyRent() != null)   l.setMonthlyRent(patch.getMonthlyRent());
    if (patch.getDepositBaht() != null)   l.setDepositBaht(patch.getDepositBaht());
    if (patch.getNotes() != null)         l.setNotes(patch.getNotes());

    // custom fields
    if (patch.getCustomName() != null)      l.setCustomName(patch.getCustomName());
    if (patch.getCustomIdCard() != null)    l.setCustomIdCard(patch.getCustomIdCard());
    if (patch.getCustomAddress() != null)   l.setCustomAddress(patch.getCustomAddress());
    if (patch.getCustomRules() != null)     l.setCustomRules(patch.getCustomRules());
    if (patch.getSettled() != null)         l.setSettled(patch.getSettled());
    if (patch.getSettledDate() != null)     l.setSettledDate(patch.getSettledDate());

    // เปลี่ยนผู้เช่า
    if (patch.getTenant() != null && patch.getTenant().getId() != null) {
      Tenant t = tenantRepo.findById(patch.getTenant().getId())
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));
      l.setTenant(t);
      // sync ไปยัง room ปัจจุบันของ lease ด้วย
      l.getRoom().setTenant(t);
      roomRepo.save(l.getRoom());
    }

    // ย้ายห้อง (รับทั้ง id หรือ number)
    if (patch.getRoom() != null && (patch.getRoom().getId() != null || patch.getRoom().getNumber() != null)) {
        Room newRoom;

        if (patch.getRoom().getNumber() != null) {
            newRoom = roomRepo.findByNumber(patch.getRoom().getNumber())
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Room number " + patch.getRoom().getNumber() + " not found"));
        } else {
            newRoom = roomRepo.findById(patch.getRoom().getId())
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Room id " + patch.getRoom().getId() + " not found"));
        }

        // ✅ Free the old room first if it exists and is different
        Room oldRoom = l.getRoom();
        if (oldRoom != null && !oldRoom.getId().equals(newRoom.getId())) {
            oldRoom.setStatus("FREE");
            oldRoom.setTenant(null);
            roomRepo.save(oldRoom);
        }

        // ✅ Assign the new room
        l.setRoom(newRoom);
    }

    // ✅ Sync room status + tenant after edit
    if (l.getRoom() != null) {
        Room room = l.getRoom();
        if (l.getTenant() != null) {
            room.setStatus("OCCUPIED");
            room.setTenant(l.getTenant());
        } else {
            room.setStatus("FREE");
            room.setTenant(null);
        }
        roomRepo.save(room);
    }

    return leaseRepo.save(l);

  }

  @Transactional
  public Lease endLease(Long id, LocalDate endDate) {
    Lease l = leaseRepo.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lease id " + id + " not found"));

    l.setStatus(Status.ENDED);
    l.setEndDate(endDate != null ? endDate : LocalDate.now());

    // เคลียร์สถานะห้องให้ว่าง
    Room room = l.getRoom();
    room.setStatus("FREE");
    room.setTenant(null);

    roomRepo.save(room);
    return leaseRepo.save(l);
  }

  /**
   * Mark ว่าเคลียร์สัญญาเรียบร้อยแล้ว (คืนเงิน/จบขั้นตอนทั้งหมด)
   */
  @Transactional
  public Lease settleLease(Long id, LocalDate date) {
    Lease l = leaseRepo.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lease id " + id + " not found"));

    l.setSettled(Boolean.TRUE);
    l.setSettledDate(date != null ? date : LocalDate.now());
    return leaseRepo.save(l);
  }

    /**
   * Delete lease and cleanup linked room state reliably.
   */
  @Transactional
  public void deleteLease(Long id) {
    Lease l = leaseRepo.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lease id " + id + " not found"));

    // Determine linked room id (safe even if l.getRoom() is a lazy proxy or detached)
    Long roomId = null;
    if (l.getRoom() != null) {
      try {
        roomId = l.getRoom().getId();
      } catch (Exception ex) {
        roomId = null;
      }
    }

    // If we have a room id, fetch the canonical Room entity from roomRepo and clear it
    if (roomId != null) {
      Room room = roomRepo.findById(roomId)
          .orElse(null);
      if (room != null) {
        room.setStatus("FREE");
        room.setTenant(null);
        roomRepo.save(room); // save before deleting lease to ensure DB state is consistent
      }
    }

    // Finally remove the lease
    leaseRepo.delete(l);
  }



}
