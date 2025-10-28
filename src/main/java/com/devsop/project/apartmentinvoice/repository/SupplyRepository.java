package com.devsop.project.apartmentinvoice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.devsop.project.apartmentinvoice.entity.Supply;

/**
 * Repository for Supply entity.
 * Supports inventory management queries.
 */
public interface SupplyRepository extends JpaRepository<Supply, Long> {

  /**
   * Find all supplies with low stock (amount < 3).
   */
  @Query("SELECT s FROM Supply s WHERE s.supplyAmount < 3")
  List<Supply> findLowStockSupplies();

  /**
   * Find supplies by name (case-insensitive search).
   */
  List<Supply> findBySupplyNameContainingIgnoreCase(String name);
}
