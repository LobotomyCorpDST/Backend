package com.devsop.project.apartmentinvoice.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.devsop.project.apartmentinvoice.entity.Supply;
import com.devsop.project.apartmentinvoice.service.SupplyService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controller for apartment supply inventory management.
 * Supports CRUD operations, search, and increment/decrement actions.
 */
@RestController
@RequestMapping("/api/supplies")
@RequiredArgsConstructor
public class SupplyController {

  private final SupplyService supplyService;

  /**
   * Get all supplies.
   * Optionally filter by name using 'search' query parameter.
   */
  @GetMapping
  public List<Supply> getAllSupplies(@RequestParam(required = false) String search) {
    if (search != null && !search.trim().isEmpty()) {
      return supplyService.searchSuppliesByName(search);
    }
    return supplyService.getAllSupplies();
  }

  /**
   * Get a single supply by ID.
   */
  @GetMapping("/{id}")
  public Supply getSupplyById(@PathVariable Long id) {
    return supplyService.getSupplyById(id);
  }

  /**
   * Get supplies with low stock (< 3 units).
   */
  @GetMapping("/low-stock")
  public List<Supply> getLowStockSupplies() {
    return supplyService.getLowStockSupplies();
  }

  /**
   * Create a new supply.
   */
  @PostMapping
  public ResponseEntity<Supply> createSupply(@Valid @RequestBody Supply supply) {
    Supply created = supplyService.createSupply(supply);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  /**
   * Update supply (name and/or amount).
   */
  @PatchMapping("/{id}")
  public Supply updateSupply(@PathVariable Long id, @RequestBody Supply updates) {
    return supplyService.updateSupply(id, updates);
  }

  /**
   * Increment supply amount by 1.
   */
  @PostMapping("/{id}/increment")
  public Supply incrementSupply(@PathVariable Long id) {
    return supplyService.incrementSupply(id);
  }

  /**
   * Decrement supply amount by 1 (minimum 0).
   */
  @PostMapping("/{id}/decrement")
  public Supply decrementSupply(@PathVariable Long id) {
    return supplyService.decrementSupply(id);
  }

  /**
   * Delete a supply.
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<?> deleteSupply(@PathVariable Long id) {
    supplyService.deleteSupply(id);
    return ResponseEntity.ok(java.util.Map.of("message", "Supply deleted successfully"));
  }
}
