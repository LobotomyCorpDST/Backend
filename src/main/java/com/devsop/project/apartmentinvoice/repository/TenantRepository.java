package com.devsop.project.apartmentinvoice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.devsop.project.apartmentinvoice.entity.Tenant;

public interface TenantRepository extends JpaRepository<Tenant, Long> {}
