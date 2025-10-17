package com.devsop.project.apartmentinvoice.controller;

import java.util.EnumSet;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.devsop.project.apartmentinvoice.entity.Maintenance.Status;
import com.devsop.project.apartmentinvoice.repository.LeaseRepository;
import com.devsop.project.apartmentinvoice.repository.MaintenanceRepository;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * สรุปข้อมูลสำหรับหน้า Dashboard
 * - ownedCount: จำนวนสัญญาเช่าที่ยัง Active ของ tenant ที่ระบุ
 * - pendingMaintenanceCount: จำนวนงานซ่อมที่ยัง "ค้างอยู่" (PLANNED + IN_PROGRESS) ทั้งระบบ
 *
 * หมายเหตุ: endpoint นี้รับ tenantId เป็นพารามิเตอร์เพื่อความง่าย
 * หากต้องการผูกกับผู้ใช้งานจาก JWT ในอนาคตค่อยปรับใน service/repo ได้โดยไม่กระทบ API นี้
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

  private final LeaseRepository leaseRepo;
  private final MaintenanceRepository maintenanceRepo;

  @GetMapping("/user-summary")
  public UserSummary getUserSummary(@RequestParam("tenantId") Long tenantId) {
    long owned = leaseRepo.countByTenant_IdAndEndDateIsNull(tenantId);

    long pending = maintenanceRepo.countByStatusIn(
        EnumSet.of(Status.PLANNED, Status.IN_PROGRESS)
    );

    return new UserSummary(owned, pending);
  }

  // ===== DTO =====
  @Getter @Setter @AllArgsConstructor
  public static class UserSummary {
    private long ownedCount;
    private long pendingMaintenanceCount;
  }
}
