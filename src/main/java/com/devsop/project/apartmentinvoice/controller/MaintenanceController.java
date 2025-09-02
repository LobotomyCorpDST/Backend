package com.devsop.project.apartmentinvoice.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.devsop.project.apartmentinvoice.dto.CreateMaintenanceRequest;
import com.devsop.project.apartmentinvoice.dto.MaintenanceResponse;
import com.devsop.project.apartmentinvoice.entity.Maintenance;
import com.devsop.project.apartmentinvoice.entity.Maintenance.Status;
import com.devsop.project.apartmentinvoice.entity.Room;
import com.devsop.project.apartmentinvoice.repository.MaintenanceRepository;
import com.devsop.project.apartmentinvoice.repository.RoomRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/maintenance")
@RequiredArgsConstructor
public class MaintenanceController {

  private final MaintenanceRepository maintenanceRepo;
  private final RoomRepository roomRepo;

  // ---------- Create ----------
  @PostMapping
  public MaintenanceResponse create(@Valid @RequestBody CreateMaintenanceRequest req) {
    Room room = roomRepo.findById(req.getRoomId()).orElseThrow();

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
  public List<MaintenanceResponse> all() {
    return maintenanceRepo.findAll().stream().map(this::toDto).toList();
  }

  @GetMapping("/{id}")
  public MaintenanceResponse getOne(@PathVariable Long id) {
    return maintenanceRepo.findById(id).map(this::toDto).orElseThrow();
  }

  @GetMapping("/by-room/{roomId}")
  public List<MaintenanceResponse> byRoom(@PathVariable Long roomId) {
    return maintenanceRepo.findByRoom_IdOrderByScheduledDateDesc(roomId)
        .stream().map(this::toDto).toList();
  }

  // ---------- Update ----------
  /** Mark as done */
  @PatchMapping("/{id}/complete")
  public MaintenanceResponse complete(
      @PathVariable Long id,
      @RequestParam("completedDate")
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate completedDate) {

    Maintenance m = maintenanceRepo.findById(id).orElseThrow();
    m.setStatus(Status.COMPLETED);           // << สำคัญ: ให้ตรงกับ enum (PLANNED, IN_PROGRESS, DONE, CANCELED)
    m.setCompletedDate(completedDate);
    m = maintenanceRepo.save(m);
    return toDto(m);
  }

  @PutMapping("/{id}")
  public MaintenanceResponse update(
      @PathVariable Long id,
      @Valid @RequestBody CreateMaintenanceRequest req) {

    Maintenance m = maintenanceRepo.findById(id).orElseThrow();

    if (!m.getRoom().getId().equals(req.getRoomId())) {
      Room room = roomRepo.findById(req.getRoomId()).orElseThrow();
      m.setRoom(room);
    }
    m.setDescription(req.getDescription());
    m.setScheduledDate(req.getScheduledDate());
    m.setCostBaht(req.getCostBaht());

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
