package com.devsop.project.apartmentinvoice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.devsop.project.apartmentinvoice.entity.InvoiceSettings;

/**
 * Repository for InvoiceSettings (singleton entity).
 * Typically only one row exists with id=1.
 */
public interface InvoiceSettingsRepository extends JpaRepository<InvoiceSettings, Long> {
}
