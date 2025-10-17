package com.devsop.project.apartmentinvoice.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.devsop.project.apartmentinvoice.entity.Maintenance;

public interface MaintenanceRepository extends JpaRepository<Maintenance, Long> {

  // ---------- ของเดิม ----------
  List<Maintenance> findByRoom_IdOrderByScheduledDateDesc(Long roomId);

  List<Maintenance> findByRoom_IdAndStatusAndCompletedDateBetween(
      Long roomId,
      Maintenance.Status status,
      LocalDate start,
      LocalDate end
  );

  // ---------- เพิ่มสำหรับ Dashboard / สรุปผล ----------
  /** นับจำนวนงานตามสถานะทั้งหมดในระบบ (เช่น PLANNED / IN_PROGRESS / COMPLETED / CANCELED) */
  long countByStatus(Maintenance.Status status);

  /** นับจำนวนงานตามหลายสถานะรวมกัน (เช่น ค้างอยู่: PLANNED + IN_PROGRESS) */
  long countByStatusIn(Collection<Maintenance.Status> statuses);

  // ---------- เสริม (เผื่อใช้รายห้อง/ฟิลเตอร์ย่อย) ----------
  long countByRoom_IdAndStatus(Long roomId, Maintenance.Status status);

  List<Maintenance> findByStatusOrderByScheduledDateAsc(Maintenance.Status status);

  List<Maintenance> findByRoom_NumberOrderByScheduledDateDesc(Integer roomNumber);
}
