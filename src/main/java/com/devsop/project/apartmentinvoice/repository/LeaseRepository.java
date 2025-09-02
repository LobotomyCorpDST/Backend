package com.devsop.project.apartmentinvoice.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.devsop.project.apartmentinvoice.entity.Lease;
import com.devsop.project.apartmentinvoice.entity.Lease.Status;

public interface LeaseRepository extends JpaRepository<Lease, Long> {

  // ---------- ใช้กับการตอบ JSON (fetch join เพื่อตัด Lazy proxy) ----------
  @Query("""
         select l
         from Lease l
         join fetch l.room
         join fetch l.tenant
         """)
  List<Lease> findAllWithRefs();

  @Query("""
         select l
         from Lease l
         join fetch l.room
         join fetch l.tenant
         where l.id = :id
         """)
  Optional<Lease> findByIdWithRefs(Long id);

  @Query("""
         select l
         from Lease l
         join fetch l.room r
         join fetch l.tenant t
         where r.id = :roomId
         """)
  List<Lease> findByRoomIdWithRefs(Long roomId);

  @Query("""
         select l
         from Lease l
         join fetch l.room r
         join fetch l.tenant t
         where t.id = :tenantId
         """)
  List<Lease> findByTenantIdWithRefs(Long tenantId);

  // ---------- เมธอดสำหรับเติมค่าเช่าอัตโนมัติ ----------
  @Query("""
         select l
         from Lease l
         join fetch l.room r
         join fetch l.tenant t
         where r.id = :roomId
           and l.startDate <= :onDate
           and (l.endDate is null or l.endDate >= :onDate)
           and l.status = com.devsop.project.apartmentinvoice.entity.Lease.Status.ACTIVE
         """)
  Optional<Lease> findActiveLeaseByRoomOnDate(Long roomId, LocalDate onDate);

  // ---------- เมธอดค้นหาปกติ (เผื่อใช้จุดอื่น) ----------
  List<Lease> findByRoom_Id(Long roomId);
  List<Lease> findByTenant_Id(Long tenantId);
  List<Lease> findByStatus(Status status);
  List<Lease> findByRoom_IdAndStatus(Long roomId, Status status);

  List<Lease> findByRoom_IdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
      Long roomId, LocalDate date1, LocalDate date2);
}
