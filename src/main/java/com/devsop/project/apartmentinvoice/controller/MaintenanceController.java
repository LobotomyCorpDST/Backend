package com.devsop.project.apartmentinvoice.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
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

import com.devsop.project.apartmentinvoice.dto.CreateMaintenanceRequest;
import com.devsop.project.apartmentinvoice.dto.MaintenanceResponse;
import com.devsop.project.apartmentinvoice.dto.UpdateMaintenanceRequest;
import com.devsop.project.apartmentinvoice.entity.Maintenance;
import com.devsop.project.apartmentinvoice.entity.Maintenance.Status;
import com.devsop.project.apartmentinvoice.entity.Room;
import com.devsop.project.apartmentinvoice.repository.MaintenanceRepository;
import com.devsop.project.apartmentinvoice.repository.RoomRepository;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RestController
@RequestMapping("/api/maintenance")
@RequiredArgsConstructor
public class MaintenanceController {

  private final MaintenanceRepository maintenanceRepo;
  private final RoomRepository roomRepo;

  // ---------- Create ----------
  @PostMapping
  public MaintenanceResponse create(@Valid @RequestBody CreateMaintenanceRequest req) {
    Room room = roomRepo.findByNumber(req.getRoomNumber())
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Room number " + req.getRoomNumber() + " not found"));

    Maintenance m = new Maintenance();
    m.setRoom(room);
    m.setDescription(req.getDescription());
    m.setStatus(Status.PLANNED);
    m.setScheduledDate(req.getScheduledDate());
    m.setCostBaht(req.getCostBaht());

    m = maintenanceRepo.save(m);
    return toDto(m);
  }

  // ---------- Read ----------
  @GetMapping
  public List<MaintenanceResponse> all(@RequestParam(required = false) String search) {
    List<Maintenance> maintenances = maintenanceRepo.findAll();

    // Filter by search term if provided
    if (search != null && !search.trim().isEmpty()) {
      String searchLower = search.toLowerCase();
      maintenances = maintenances.stream()
          .filter(m ->
              m.getId().toString().contains(search) ||
              (m.getRoom() != null && m.getRoom().getNumber().toString().contains(search)) ||
              (m.getDescription() != null && m.getDescription().toLowerCase().contains(searchLower)) ||
              (m.getStatus() != null && m.getStatus().toString().toLowerCase().contains(searchLower))
          )
          .toList();
    }

    return maintenances.stream().map(this::toDto).toList();
  }

  @GetMapping("/{id}")
  public MaintenanceResponse getOne(@PathVariable Long id) {
    return maintenanceRepo.findById(id)
        .map(this::toDto)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Maintenance id " + id + " not found"));
  }

  // (ใหม่) ดูตาม "เลขห้อง"
  @GetMapping("/by-room-number/{roomNumber}")
  public List<MaintenanceResponse> byRoomNumber(@PathVariable Integer roomNumber) {
    return maintenanceRepo.findByRoom_NumberOrderByScheduledDateDesc(roomNumber)
        .stream().map(this::toDto).toList();
  }

  // (คง endpoint เก่าไว้ถ้ายังมี frontend เรียกอยู่ — แต่ควรเลิกใช้)
  @GetMapping("/by-room/{roomId}")
  @Deprecated
  public List<MaintenanceResponse> byRoom(@PathVariable Long roomId) {
    return maintenanceRepo.findByRoom_IdOrderByScheduledDateDesc(roomId)
        .stream().map(this::toDto).toList();
  }

  // ---------- Update / Complete ----------
  /** Mark as completed (จบงาน) */
  @PatchMapping("/{id}/complete")
  public MaintenanceResponse complete(
      @PathVariable Long id,
      @RequestParam("completedDate")
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate completedDate) {

    Maintenance m = maintenanceRepo.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Maintenance id " + id + " not found"));
    m.setStatus(Status.COMPLETED);
    m.setCompletedDate(completedDate);
    m = maintenanceRepo.save(m);
    return toDto(m);
  }

  /** แก้ไขข้อมูล (ADMIN edit): อนุญาตแก้เฉพาะบางฟิลด์ */
  @PatchMapping("/{id}/edit")
  public MaintenanceResponse adminEdit(@PathVariable Long id, @RequestBody EditMaintenanceRequest req) {
    Maintenance m = maintenanceRepo.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Maintenance id " + id + " not found"));

    if (req.getScheduledDate() != null) m.setScheduledDate(req.getScheduledDate());
    if (req.getCompletedDate() != null) m.setCompletedDate(req.getCompletedDate());
    if (req.getDescription() != null)   m.setDescription(req.getDescription());
    if (req.getCostBaht() != null)      m.setCostBaht(req.getCostBaht());

    if (req.getStatus() != null) {
      try {
        m.setStatus(Status.valueOf(req.getStatus().toUpperCase()));
      } catch (IllegalArgumentException ex) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status: " + req.getStatus());
      }
    }

    m = maintenanceRepo.save(m);
    return toDto(m);
  }

  @Getter @Setter @NoArgsConstructor @AllArgsConstructor
  public static class EditMaintenanceRequest {
    private String description;
    private LocalDate scheduledDate;
    private LocalDate completedDate;
    private BigDecimal costBaht;
    /** PLANNED / IN_PROGRESS / COMPLETED / CANCELED */
    private String status;
  }

// ---------- Update ----------

// ใช้ DTO ใหม่ในการอัปเดต (ไม่บังคับทุกฟิลด์)
@PutMapping("/{id}")
public MaintenanceResponse update(
    @PathVariable Long id,
    @Valid @RequestBody UpdateMaintenanceRequest req) {

  Maintenance m = maintenanceRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(
          HttpStatus.NOT_FOUND, "Maintenance id " + id + " not found"));

  // ถ้ามี roomNumber และต่างจากห้องเดิม → อัปเดตห้อง
  if (req.getRoomNumber() != null && !req.getRoomNumber().equals(m.getRoom().getNumber())) {
    Room room = roomRepo.findByNumber(req.getRoomNumber())
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Room number " + req.getRoomNumber() + " not found"));
    m.setRoom(room);
  }

  if (req.getDescription() != null) m.setDescription(req.getDescription());
  if (req.getScheduledDate() != null) m.setScheduledDate(req.getScheduledDate());
  if (req.getCompletedDate() != null) m.setCompletedDate(req.getCompletedDate());
  if (req.getCostBaht() != null) m.setCostBaht(req.getCostBaht());
  // ปรับสถานะ (และจัดการ completedDate)
  if (req.getStatus() != null) {
  try {
    Status newStatus = Status.valueOf(req.getStatus().toUpperCase());
    m.setStatus(newStatus);

    // ถ้าสถานะเป็น COMPLETED → ตั้ง completedDate = วันนี้
    if (newStatus == Status.COMPLETED) {
      m.setCompletedDate(LocalDate.now());
    } 
    // ถ้าเปลี่ยนเป็นสถานะอื่น → ล้าง completedDate
    else {
      m.setCompletedDate(null);
    }

  } catch (IllegalArgumentException ex) {
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status: " + req.getStatus());
  }
  }


  m = maintenanceRepo.save(m);
  return toDto(m);
  }


  // ---------- mapper ----------
  private MaintenanceResponse toDto(Maintenance m) {
    return MaintenanceResponse.builder()
        .id(m.getId())
        .roomId(m.getRoom().getId())
        .roomNumber(m.getRoom().getNumber())
        .description(m.getDescription())
        .status(m.getStatus())
        .scheduledDate(m.getScheduledDate())
        .completedDate(m.getCompletedDate())
        .costBaht(m.getCostBaht())
        .build();
  }
}
