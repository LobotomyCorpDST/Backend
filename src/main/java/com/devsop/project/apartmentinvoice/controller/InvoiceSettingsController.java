package com.devsop.project.apartmentinvoice.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.devsop.project.apartmentinvoice.entity.InvoiceSettings;
import com.devsop.project.apartmentinvoice.service.InvoiceSettingsService;

import lombok.RequiredArgsConstructor;

/**
 * Controller for managing invoice settings (payment description, QR code, interest rate).
 * Endpoints: GET, PUT /api/invoice-settings, POST /api/invoice-settings/qr-upload
 */
@RestController
@RequestMapping("/api/invoice-settings")
@RequiredArgsConstructor
public class InvoiceSettingsController {

  private final InvoiceSettingsService settingsService;

  @Value("${file.upload.dir:./uploads}")
  private String uploadBaseDir;

  private static final long MAX_QR_FILE_SIZE = 10 * 1024 * 1024; // 10MB

  /**
   * Get current invoice settings.
   */
  @GetMapping
  public InvoiceSettings getSettings() {
    return settingsService.getSettings();
  }

  /**
   * Update invoice settings (payment description, interest rate).
   * QR code is updated separately via /qr-upload endpoint.
   */
  @PutMapping
  public InvoiceSettings updateSettings(@RequestBody InvoiceSettings settings) {
    return settingsService.updateSettings(settings);
  }

  /**
   * Upload QR code image for payment.
   * Replaces existing QR code if one exists.
   */
  @PostMapping("/qr-upload")
  public ResponseEntity<?> uploadQrCode(@RequestParam("file") MultipartFile file) {
    // Validate file
    if (file.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "QR code file is empty");
    }

    if (file.getSize() > MAX_QR_FILE_SIZE) {
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "QR code file size exceeds maximum limit of 10MB"
      );
    }

    String contentType = file.getContentType();
    if (contentType == null || !(contentType.equals("image/jpeg") ||
                                   contentType.equals("image/png") ||
                                   contentType.equals("image/jpg"))) {
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "QR code must be a JPG or PNG image"
      );
    }

    // Generate unique filename
    String originalFilename = file.getOriginalFilename();
    String extension = "";
    if (originalFilename != null && originalFilename.contains(".")) {
      extension = originalFilename.substring(originalFilename.lastIndexOf("."));
    }

    String uniqueFilename = "qr_" + UUID.randomUUID().toString() + extension;

    // Build file path: uploads/qr/{uniqueFilename}
    String relativePath = "qr/" + uniqueFilename;
    Path uploadPath = Paths.get(uploadBaseDir, "qr");
    Path filePath = uploadPath.resolve(uniqueFilename);

    try {
      // Create directory if it doesn't exist
      Files.createDirectories(uploadPath);

      // Delete old QR code if it exists
      InvoiceSettings currentSettings = settingsService.getSettings();
      if (currentSettings.getQrCodeImagePath() != null) {
        Path oldQrPath = Paths.get(uploadBaseDir, currentSettings.getQrCodeImagePath());
        try {
          Files.deleteIfExists(oldQrPath);
        } catch (IOException e) {
          System.err.println("Warning: Failed to delete old QR code: " + e.getMessage());
        }
      }

      // Save new QR code
      Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

      // Update settings with new QR path
      InvoiceSettings updated = settingsService.updateQrCodePath(relativePath);

      return ResponseEntity.ok(Map.of(
        "message", "QR code uploaded successfully",
        "qrCodePath", relativePath,
        "settings", updated
      ));

    } catch (IOException e) {
      throw new ResponseStatusException(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Failed to save QR code: " + e.getMessage()
      );
    }
  }

  // Helper for Map.of (Java 9+)
  private static class Map {
    public static <K, V> java.util.Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3) {
      return java.util.Map.of(k1, v1, k2, v2, k3, v3);
    }
  }
}
