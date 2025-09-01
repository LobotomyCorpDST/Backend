package com.devsop.project.apartmentinvoice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.devsop.project.apartmentinvoice.entity.Invoice;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
  List<Invoice> findByRoom_Id(Long roomId);
  List<Invoice> findByTenant_Id(Long tenantId);
  List<Invoice> findByBillingYearAndBillingMonth(Integer year, Integer month);
  Optional<Invoice> findFirstByRoom_IdAndBillingYearAndBillingMonth(Long roomId, Integer year, Integer month);
}
