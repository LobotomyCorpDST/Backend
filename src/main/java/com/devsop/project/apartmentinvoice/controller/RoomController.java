package com.devsop.project.apartmentinvoice.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
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
import org.springframework.web.server.ResponseStatusException;

import com.devsop.project.apartmentinvoice.entity.Lease;
import com.devsop.project.apartmentinvoice.entity.Lease.Status;
import com.devsop.project.apartmentinvoice.entity.Room;
import com.devsop.project.apartmentinvoice.repository.LeaseRepository;
import com.devsop.project.apartmentinvoice.repository.RoomRepository;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

  private final RoomRepository roomRepo;
  private final LeaseRepository leaseRepo;

  // =================== DTO ===================
  @Getter @Setter @NoArgsConstructor @AllArgsConstructor
  public static class RoomView {
    private Long id;
    private Integer number;
    private String status;
    private Boolean isOwned;
    private String tenantName;
  }

  private static RoomView toView(Room r, Lease activeLease) {
    boolean owned = (activeLease != null && activeLease.getStatus() == Status.ACTIVE);
    String tenantName = (activeLease != null && activeLease.getTenant() != null)
        ? activeLease.getTenant().getName()
        : null;
    return new RoomView(r.getId(), r.getNumber(), r.getStatus(), owned, tenantName);
  }

  // =================== CRUD ===================

  /** ดึงห้องทั้งหมด + สรุปสถานะผู้เช่าปัจจุบัน (RoomView) - รองรับการค้นหา */
  @GetMapping
  @Transactional(readOnly = true)
  public List<RoomView> all(@RequestParam(required = false) String search) {
    List<Room> rooms;

    if (search != null && !search.trim().isEmpty()) {
      // Search by room number or tenant name
      rooms = roomRepo.findAll().stream()
          .filter(r -> {
            // Match room number
            if (r.getNumber().toString().contains(search)) {
              return true;
            }
            // Match tenant name
            Lease active = leaseRepo.findByRoom_IdAndStatus(r.getId(), Status.ACTIVE)
                .stream().findFirst().orElse(null);
            if (active != null && active.getTenant() != null) {
              String tenantName = active.getTenant().getName();
              return tenantName != null && tenantName.toLowerCase().contains(search.toLowerCase());
            }
            return false;
          })
          .collect(Collectors.toList());
    } else {
      rooms = roomRepo.findAll();
    }

    return rooms.stream()
        .map(r -> {
          Lease active = leaseRepo.findByRoom_IdAndStatus(r.getId(), Status.ACTIVE)
              .stream().findFirst().orElse(null);
          return toView(r, active);
        })
        .collect(Collectors.toList());
  }

  /** สร้างห้องใหม่ */
  @PostMapping
  public Room create(@RequestBody Room r) {
    if (r.getNumber() == null)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "number is required");
    if (roomRepo.existsByNumber(r.getNumber()))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Room number already exists");
    if (r.getStatus() == null)
      r.setStatus("FREE");
    return roomRepo.save(r);
  }

  /** ดึงห้องตามเลขห้อง (RoomView) */
  @GetMapping("/by-number/{number}")
  @Transactional(readOnly = true)
  public RoomView byNumber(@PathVariable Integer number) {
    Room room = roomRepo.findByNumber(number)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    Lease active = leaseRepo.findByRoom_IdAndStatus(room.getId(), Status.ACTIVE)
        .stream().findFirst().orElse(null);

    return toView(room, active);
  }

  /** ดึงห้องตาม id (ส่ง Entity ตรง ๆ ใช้กับหน้าแก้ไข) */
  @GetMapping("/{id}")
  @Transactional(readOnly = true)
  public Room byId(@PathVariable Long id) {
    return roomRepo.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
  }

  /** แก้ไขหมายเลขห้อง/สถานะ (กันเลขซ้ำ) */
  @PutMapping("/{id}")
  public Room update(@PathVariable Long id, @RequestBody Room patch) {
    Room r = roomRepo.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

    // อัปเดตหมายเลขห้อง (ถ้ามีส่งมาและเปลี่ยนจริง)
    if (patch.getNumber() != null && !patch.getNumber().equals(r.getNumber())) {
      if (roomRepo.existsByNumber(patch.getNumber())) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Room number already exists");
      }
      r.setNumber(patch.getNumber());
    }

    // อัปเดตสถานะห้อง (ถ้ามีส่งมา)
    if (patch.getStatus() != null) {
      r.setStatus(patch.getStatus());
    }

    return roomRepo.save(r);
  }

  /** ✅ ลบห้องตาม id */
  @DeleteMapping("/{id}")
  public void delete(@PathVariable Long id) {
    Room room = roomRepo.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
    roomRepo.delete(room);
  }

  @GetMapping("/ping")
  public String ping() { return "ok"; }
}
