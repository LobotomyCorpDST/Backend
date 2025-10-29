package com.devsop.project.apartmentinvoice.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.devsop.project.apartmentinvoice.entity.Supply;
import com.devsop.project.apartmentinvoice.repository.SupplyRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service for managing apartment supply inventory.
 */
@Service
@RequiredArgsConstructor
public class SupplyService {

  private final SupplyRepository supplyRepository;

  /**
   * Get all supplies.
   */
  public List<Supply> getAllSupplies() {
    return supplyRepository.findAll();
  }

  /**
   * Get supply by ID.
   */
  public Supply getSupplyById(Long id) {
    return supplyRepository.findById(id)
      .orElseThrow(() -> new ResponseStatusException(
        HttpStatus.NOT_FOUND,
        "Supply not found: " + id
      ));
  }

  /**
   * Get supplies with low stock (< 3 units).
   */
  public List<Supply> getLowStockSupplies() {
    return supplyRepository.findLowStockSupplies();
  }

  /**
   * Search supplies by name.
   */
  public List<Supply> searchSuppliesByName(String name) {
    if (name == null || name.trim().isEmpty()) {
      return getAllSupplies();
    }
    return supplyRepository.findBySupplyNameContainingIgnoreCase(name.trim());
  }

  /**
   * Create a new supply.
   */
  public Supply createSupply(Supply supply) {
    if (supply.getSupplyName() == null || supply.getSupplyName().trim().isEmpty()) {
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "Supply name is required"
      );
    }
    if (supply.getSupplyAmount() == null) {
      supply.setSupplyAmount(0);
    }
    if (supply.getSupplyAmount() < 0) {
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "Supply amount cannot be negative"
      );
    }
    return supplyRepository.save(supply);
  }

  /**
   * Update supply (name and/or amount).
   */
  public Supply updateSupply(Long id, Supply updates) {
    Supply existing = getSupplyById(id);

    if (updates.getSupplyName() != null && !updates.getSupplyName().trim().isEmpty()) {
      existing.setSupplyName(updates.getSupplyName().trim());
    }

    if (updates.getSupplyAmount() != null) {
      if (updates.getSupplyAmount() < 0) {
        throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Supply amount cannot be negative"
        );
      }
      existing.setSupplyAmount(updates.getSupplyAmount());
    }

    return supplyRepository.save(existing);
  }

  /**
   * Increment supply amount by 1.
   */
  public Supply incrementSupply(Long id) {
    Supply supply = getSupplyById(id);
    supply.setSupplyAmount(supply.getSupplyAmount() + 1);
    return supplyRepository.save(supply);
  }

  /**
   * Decrement supply amount by 1 (minimum 0).
   */
  public Supply decrementSupply(Long id) {
    Supply supply = getSupplyById(id);
    if (supply.getSupplyAmount() > 0) {
      supply.setSupplyAmount(supply.getSupplyAmount() - 1);
    }
    return supplyRepository.save(supply);
  }

  /**
   * Delete supply.
   */
  public void deleteSupply(Long id) {
    if (!supplyRepository.existsById(id)) {
      throw new ResponseStatusException(
        HttpStatus.NOT_FOUND,
        "Supply not found: " + id
      );
    }
    supplyRepository.deleteById(id);
  }
}
