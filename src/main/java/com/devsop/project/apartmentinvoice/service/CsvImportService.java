package com.devsop.project.apartmentinvoice.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.devsop.project.apartmentinvoice.entity.Invoice;
import com.devsop.project.apartmentinvoice.entity.Lease;
import com.devsop.project.apartmentinvoice.entity.Maintenance;
import com.devsop.project.apartmentinvoice.entity.Maintenance.Status;
import com.devsop.project.apartmentinvoice.entity.Room;
import com.devsop.project.apartmentinvoice.metrics.InvoiceMetrics;
import com.devsop.project.apartmentinvoice.repository.InvoiceRepository;
import com.devsop.project.apartmentinvoice.repository.LeaseRepository;
import com.devsop.project.apartmentinvoice.repository.MaintenanceRepository;
import com.devsop.project.apartmentinvoice.repository.RoomRepository;
import com.devsop.project.apartmentinvoice.service.InvoiceService.DebtCalculation;
import com.devsop.project.apartmentinvoice.service.storage.StorageService;

import lombok.RequiredArgsConstructor;

/**
 * Service for importing invoices from CSV files.
 * CSV Format: Room Number, Electricity Units, Water Units, Billing Month, Billing Year, Electricity Rate, Water Rate
 */
@Service
@RequiredArgsConstructor
public class CsvImportService {

  private final InvoiceRepository invoiceRepository;
  private final RoomRepository roomRepository;
  private final LeaseRepository leaseRepository;
  private final MaintenanceRepository maintenanceRepository;
  private final InvoiceService invoiceService;
  private final InvoiceMetrics invoiceMetrics;
  private final StorageService storageService;

  /**
   * Import invoices from CSV file.
   *
   * @param file CSV file uploaded
   * @return ImportResult with success/failure counts and error details
   */
  public ImportResult importInvoicesFromCsv(MultipartFile file) {
    uploadCsvToStorage(file);
    return invoiceMetrics.recordImport(() -> importCsvInternal(file));
  }

  private void uploadCsvToStorage(MultipartFile file) {
    if (file.isEmpty()) {
      return;
    }

    try {
      String originalFilename = file.getOriginalFilename();
      if (originalFilename == null || originalFilename.isEmpty()) {
        originalFilename = "unknown_import.csv";
      }

      String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
      String uuidPart = UUID.randomUUID().toString().substring(0, 8);
      String uniqueFilename = String.format("%s_%s_%s", timestamp, uuidPart, originalFilename);

      String filePath = "imports/csv/" + uniqueFilename;
      String contentType = file.getContentType() != null ? file.getContentType() : "text/csv";

      storageService.uploadFile(file.getInputStream(), uniqueFilename, contentType, filePath);
    } catch (IOException e) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Failed to upload CSV backup to storage: " + e.getMessage(),
          e
      );
    }
  }

  private ImportResult importCsvInternal(MultipartFile file) {
    if (file.isEmpty()) {
      invoiceMetrics.incrementImportErrors();
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CSV file is empty");
    }

    String originalName = file.getOriginalFilename();
    if (originalName != null && !originalName.toLowerCase().endsWith(".csv")) {
      invoiceMetrics.incrementImportErrors();
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File must be a CSV file");
    }

    ImportResult result = new ImportResult();
    int lineNumber = 0;

    try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
      String line;
      boolean isFirstLine = true;

      while ((line = reader.readLine()) != null) {
        lineNumber++;

        if (isFirstLine && line.toLowerCase().contains("room")) {
          isFirstLine = false;
          continue;
        }
        isFirstLine = false;

        if (line.trim().isEmpty()) {
          continue;
        }

        try {
          processLine(line, lineNumber, result);
        } catch (Exception e) {
          recordImportError(result, lineNumber, "Error processing line: " + e.getMessage());
        }
      }

    } catch (Exception e) {
      invoiceMetrics.incrementImportErrors();
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Failed to read CSV file: " + e.getMessage()
      );
    }

    return result;
  }

  private void recordImportError(ImportResult result, int lineNumber, String message) {
    result.addError(lineNumber, message);
    invoiceMetrics.incrementImportErrors();
  }

  private void processLine(String line, int lineNumber, ImportResult result) {
    String[] parts = line.split(",");

    if (parts.length < 7) {
      recordImportError(result, lineNumber, "Invalid CSV format. Expected 7 columns, found " + parts.length);
      return;
    }

    try {
      Integer roomNumber = Integer.parseInt(parts[0].trim());
      BigDecimal electricityUnits = new BigDecimal(parts[1].trim());
      BigDecimal waterUnits = new BigDecimal(parts[2].trim());
      Integer billingMonth = Integer.parseInt(parts[3].trim());
      Integer billingYear = Integer.parseInt(parts[4].trim());
      BigDecimal electricityRate = new BigDecimal(parts[5].trim());
      BigDecimal waterRate = new BigDecimal(parts[6].trim());

      if (billingMonth < 1 || billingMonth > 12) {
        recordImportError(result, lineNumber, "Invalid billing month: " + billingMonth);
        return;
      }

      Optional<Room> roomOpt = roomRepository.findByNumber(roomNumber);
      if (roomOpt.isEmpty()) {
        recordImportError(result, lineNumber, "Room not found: " + roomNumber);
        return;
      }
      Room room = roomOpt.get();

      Optional<Invoice> existingInvoice = invoiceRepository.findFirstByRoom_IdAndBillingYearAndBillingMonth(
          room.getId(),
          billingYear,
          billingMonth
      );
      if (existingInvoice.isPresent()) {
        recordImportError(result, lineNumber, "Invoice already exists for room " + roomNumber + " in " + billingYear + "-" + billingMonth);
        return;
      }

      LocalDate issueDate = LocalDate.of(billingYear, billingMonth, 1);
      Lease lease = leaseRepository.findActiveLeaseByRoomOnDate(room.getId(), issueDate).orElse(null);

      var tenant = (lease != null) ? lease.getTenant() : room.getTenant();
      if (tenant == null) {
        recordImportError(result, lineNumber, "Room " + roomNumber + " has no tenant assigned");
        return;
      }

      DebtCalculation debt = invoiceService.calculateAccumulatedDebt(room.getId(), billingYear, billingMonth);

      Invoice invoice = new Invoice();
      invoice.setRoom(room);
      invoice.setTenant(tenant);
      invoice.setBillingYear(billingYear);
      invoice.setBillingMonth(billingMonth);
      invoice.setIssueDate(issueDate);
      invoice.setDueDate(issueDate.plusDays(7));

      invoice.setElectricityUnits(electricityUnits);
      invoice.setElectricityRate(electricityRate);
      invoice.setElectricityBaht(electricityUnits.multiply(electricityRate));

      invoice.setWaterUnits(waterUnits);
      invoice.setWaterRate(waterRate);
      invoice.setWaterBaht(waterUnits.multiply(waterRate));

      invoice.setRentBaht(lease != null && lease.getMonthlyRent() != null ? lease.getMonthlyRent() : BigDecimal.ZERO);
      invoice.setCommonFeeBaht(room.getCommonFeeBaht() != null ? room.getCommonFeeBaht() : BigDecimal.ZERO);
      invoice.setGarbageFeeBaht(room.getGarbageFeeBaht() != null ? room.getGarbageFeeBaht() : BigDecimal.ZERO);
      invoice.setOtherBaht(BigDecimal.ZERO);

      LocalDate firstDay = LocalDate.of(billingYear, billingMonth, 1);
      LocalDate lastDay = firstDay.withDayOfMonth(firstDay.lengthOfMonth());

      List<Maintenance> maintenanceItems = maintenanceRepository
          .findByRoom_IdAndStatusAndCompletedDateBetween(room.getId(), Status.COMPLETED, firstDay, lastDay);

      BigDecimal maintenanceSum = maintenanceItems.stream()
          .map(Maintenance::getCostBaht)
          .filter(c -> c != null)
          .reduce(BigDecimal.ZERO, BigDecimal::add);

      invoice.setMaintenanceBaht(maintenanceSum);

      BigDecimal currentTotal = BigDecimal.ZERO;
      currentTotal = currentTotal.add(sum(invoice.getRentBaht()));
      currentTotal = currentTotal.add(sum(invoice.getElectricityBaht()));
      currentTotal = currentTotal.add(sum(invoice.getWaterBaht()));
      currentTotal = currentTotal.add(sum(invoice.getCommonFeeBaht()));
      currentTotal = currentTotal.add(sum(invoice.getGarbageFeeBaht()));
      currentTotal = currentTotal.add(sum(invoice.getOtherBaht()));
      currentTotal = currentTotal.add(sum(invoice.getMaintenanceBaht()));

      invoice.setTotalBaht(currentTotal);

      invoice.setPreviousBalance(debt.getPreviousBalance());
      invoice.setInterestCharge(debt.getInterestCharge());
      invoice.setAccumulatedTotal(currentTotal.add(debt.getPreviousBalance()).add(debt.getInterestCharge()));

      invoiceRepository.save(invoice);
      result.incrementSuccess();
      invoiceMetrics.incrementInvoiceCreated();

    } catch (NumberFormatException e) {
      recordImportError(result, lineNumber, "Invalid number format: " + e.getMessage());
    } catch (Exception e) {
      recordImportError(result, lineNumber, "Unexpected error: " + e.getMessage());
    }
  }

  private BigDecimal sum(BigDecimal val) {
    return val != null ? val : BigDecimal.ZERO;
  }

  public static class ImportResult {
    private int successCount = 0;
    private final List<String> errors = new ArrayList<>();

    public void incrementSuccess() {
      successCount++;
    }

    public void addError(int lineNumber, String message) {
      errors.add("Line " + lineNumber + ": " + message);
    }

    public int getSuccessCount() {
      return successCount;
    }

    public int getFailureCount() {
      return errors.size();
    }

    public List<String> getErrors() {
      return errors;
    }

    public int getTotalProcessed() {
      return successCount + errors.size();
    }
  }
}
