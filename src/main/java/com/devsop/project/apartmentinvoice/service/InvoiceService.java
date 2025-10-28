package com.devsop.project.apartmentinvoice.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;

import com.devsop.project.apartmentinvoice.entity.Invoice;
import com.devsop.project.apartmentinvoice.entity.Invoice.Status;
import com.devsop.project.apartmentinvoice.entity.InvoiceSettings;
import com.devsop.project.apartmentinvoice.repository.InvoiceRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service for invoice business logic including debt accumulation and interest calculation.
 */
@Service
@RequiredArgsConstructor
public class InvoiceService {

  private final InvoiceRepository invoiceRepository;
  private final InvoiceSettingsService settingsService;

  /**
   * Calculate accumulated debt (unpaid balance + interest) for a room.
   * Used when creating a new invoice to include outstanding amounts from previous periods.
   *
   * @param roomId Room ID
   * @param billingYear Billing year of the NEW invoice being created
   * @param billingMonth Billing month of the NEW invoice being created
   * @return DebtCalculation with previousBalance, interestCharge, and accumulatedTotal
   */
  public DebtCalculation calculateAccumulatedDebt(Long roomId, Integer billingYear, Integer billingMonth) {
    // Get unpaid invoices from previous periods
    List<Invoice> unpaidInvoices = invoiceRepository.findUnpaidInvoicesByRoomBeforePeriod(
      roomId,
      billingYear,
      billingMonth,
      Status.PAID
    );

    if (unpaidInvoices.isEmpty()) {
      return new DebtCalculation(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    // Get interest rate from settings
    InvoiceSettings settings = settingsService.getSettings();
    BigDecimal interestRatePerMonth = settings.getInterestRatePerMonth();
    if (interestRatePerMonth == null) {
      interestRatePerMonth = BigDecimal.ZERO;
    }

    BigDecimal totalPreviousBalance = BigDecimal.ZERO;
    BigDecimal totalInterest = BigDecimal.ZERO;

    LocalDate newInvoiceDate = LocalDate.of(billingYear, billingMonth, 1);

    for (Invoice unpaidInvoice : unpaidInvoices) {
      // Get original total (or accumulatedTotal if this invoice already had debt)
      BigDecimal originalAmount = unpaidInvoice.getTotalBaht();
      if (originalAmount == null) {
        originalAmount = BigDecimal.ZERO;
      }

      totalPreviousBalance = totalPreviousBalance.add(originalAmount);

      // Calculate interest based on months overdue
      if (unpaidInvoice.getDueDate() != null) {
        LocalDate dueDate = unpaidInvoice.getDueDate();
        long monthsOverdue = ChronoUnit.MONTHS.between(dueDate, newInvoiceDate);

        // Interest only accrues starting from the next billing cycle after due date
        if (monthsOverdue > 0) {
          // Simple interest: principal × rate × time
          BigDecimal interest = originalAmount
            .multiply(interestRatePerMonth)
            .multiply(BigDecimal.valueOf(monthsOverdue))
            .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);

          totalInterest = totalInterest.add(interest);
        }
      }
    }

    BigDecimal accumulatedTotal = totalPreviousBalance.add(totalInterest);

    return new DebtCalculation(totalPreviousBalance, totalInterest, accumulatedTotal);
  }

  /**
   * Get all invoices for the current calendar month.
   * Used for "select current month" bulk print feature.
   */
  public List<Invoice> getInvoicesForCurrentMonth() {
    LocalDate now = LocalDate.now();
    return invoiceRepository.findByBillingYearAndBillingMonth(now.getYear(), now.getMonthValue());
  }

  /**
   * DTO for debt calculation result.
   */
  public static class DebtCalculation {
    private final BigDecimal previousBalance;
    private final BigDecimal interestCharge;
    private final BigDecimal accumulatedTotal;

    public DebtCalculation(BigDecimal previousBalance, BigDecimal interestCharge, BigDecimal accumulatedTotal) {
      this.previousBalance = previousBalance;
      this.interestCharge = interestCharge;
      this.accumulatedTotal = accumulatedTotal;
    }

    public BigDecimal getPreviousBalance() {
      return previousBalance;
    }

    public BigDecimal getInterestCharge() {
      return interestCharge;
    }

    public BigDecimal getAccumulatedTotal() {
      return accumulatedTotal;
    }
  }
}
