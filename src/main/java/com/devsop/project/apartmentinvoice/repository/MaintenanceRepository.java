package com.devsop.project.apartmentinvoice.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.devsop.project.apartmentinvoice.entity.Maintenance;
import com.devsop.project.apartmentinvoice.dto.MaintenanceDueDto;

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

  // ---------- ✅ ใหม่: สำหรับ Notification (Maintenance ถึงกำหนดวันนี้) ----------
  @Query("""
    select new com.devsop.project.apartmentinvoice.dto.MaintenanceDueDto(
      m.id,
      m.description,
      r.number,
      m.scheduledDate,
      m.status
    )
    from Maintenance m
    join m.room r
    where m.scheduledDate = :date
    order by r.number asc
  """)
  List<MaintenanceDueDto> findDueOn(@Param("date") LocalDate date);
}
