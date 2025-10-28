package com.devsop.project.apartmentinvoice.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.devsop.project.apartmentinvoice.entity.InvoiceSettings;
import com.devsop.project.apartmentinvoice.repository.InvoiceSettingsRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service for managing apartment-wide invoice settings.
 * This is a singleton entity - only one settings record should exist.
 */
@Service
@RequiredArgsConstructor
public class InvoiceSettingsService {

  private final InvoiceSettingsRepository settingsRepository;

  /**
   * Get current invoice settings.
   * If no settings exist, returns default values.
   */
  public InvoiceSettings getSettings() {
    return settingsRepository.findById(1L)
      .orElseGet(this::createDefaultSettings);
  }

  /**
   * Update invoice settings.
   * Creates new settings if none exist.
   */
  public InvoiceSettings updateSettings(InvoiceSettings settings) {
    InvoiceSettings existing = settingsRepository.findById(1L).orElse(null);

    if (existing != null) {
      // Update existing settings
      if (settings.getPaymentDescription() != null) {
        existing.setPaymentDescription(settings.getPaymentDescription());
      }
      if (settings.getQrCodeImagePath() != null) {
        existing.setQrCodeImagePath(settings.getQrCodeImagePath());
      }
      if (settings.getInterestRatePerMonth() != null) {
        existing.setInterestRatePerMonth(settings.getInterestRatePerMonth());
      }
      return settingsRepository.save(existing);
    } else {
      // Create new settings with id=1
      settings.setId(1L);
      return settingsRepository.save(settings);
    }
  }

  /**
   * Update only the QR code image path.
   */
  public InvoiceSettings updateQrCodePath(String qrCodePath) {
    InvoiceSettings settings = getSettings();
    settings.setQrCodeImagePath(qrCodePath);
    return settingsRepository.save(settings);
  }

  /**
   * Create default settings with placeholder values.
   */
  private InvoiceSettings createDefaultSettings() {
    InvoiceSettings defaults = new InvoiceSettings();
    defaults.setId(1L);
    defaults.setPaymentDescription(
      "ธนาคารกสิกรไทย\n" +
      "บัญชีออมทรัพย์\n" +
      "เลขที่ 123-4-56789-0\n" +
      "ชื่อบัญชี: อพาร์ทเมนต์ABC"
    );
    defaults.setQrCodeImagePath("qr/default_qr.png");
    defaults.setInterestRatePerMonth(new BigDecimal("2.00")); // 2% per month
    return settingsRepository.save(defaults);
  }
}
