package com.devsop.project.apartmentinvoice.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.devsop.project.apartmentinvoice.entity.Maintenance;

public interface MaintenanceRepository extends JpaRepository<Maintenance, Long> {

  List<Maintenance> findByRoom_IdOrderByScheduledDateDesc(Long roomId);

  List<Maintenance> findByRoom_IdAndStatusAndCompletedDateBetween(
      Long roomId,
      Maintenance.Status status,
      LocalDate start,
      LocalDate end
  );
}
