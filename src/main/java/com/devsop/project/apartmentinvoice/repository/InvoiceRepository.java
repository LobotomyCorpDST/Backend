package com.devsop.project.apartmentinvoice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.devsop.project.apartmentinvoice.entity.Invoice;
import com.devsop.project.apartmentinvoice.entity.Invoice.Status;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
  List<Invoice> findByRoom_Id(Long roomId);
  List<Invoice> findByTenant_Id(Long tenantId);
  List<Invoice> findByBillingYearAndBillingMonth(Integer year, Integer month);
  Optional<Invoice> findFirstByRoom_IdAndBillingYearAndBillingMonth(Long roomId, Integer year, Integer month);

  /**
   * Find all unpaid invoices for a specific room before a given billing period.
   * Used for calculating accumulated debt.
   */
  @Query("SELECT i FROM Invoice i WHERE i.room.id = :roomId " +
         "AND i.status <> :paidStatus " +
         "AND (i.billingYear < :year OR (i.billingYear = :year AND i.billingMonth < :month)) " +
         "ORDER BY i.billingYear, i.billingMonth")
  List<Invoice> findUnpaidInvoicesByRoomBeforePeriod(
    @Param("roomId") Long roomId,
    @Param("year") Integer year,
    @Param("month") Integer month,
    @Param("paidStatus") Status paidStatus
  );
}
