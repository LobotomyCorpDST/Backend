package com.devsop.project.apartmentinvoice.dto;

import java.time.LocalDate;

import com.devsop.project.apartmentinvoice.entity.Maintenance;

public record MaintenanceDueDto(
    Long id,
    String description,
    Integer roomNumber,
    LocalDate scheduledDate,
    Maintenance.Status status
) {}
