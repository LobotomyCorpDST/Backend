package com.devsop.project.apartmentinvoice.unit.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.devsop.project.apartmentinvoice.entity.Invoice;
import com.devsop.project.apartmentinvoice.entity.Invoice.Status;
import com.devsop.project.apartmentinvoice.entity.InvoiceSettings;
import com.devsop.project.apartmentinvoice.entity.Room;
import com.devsop.project.apartmentinvoice.entity.Tenant;
import com.devsop.project.apartmentinvoice.repository.InvoiceRepository;
import com.devsop.project.apartmentinvoice.service.InvoiceService;
import com.devsop.project.apartmentinvoice.service.InvoiceService.DebtCalculation;
import com.devsop.project.apartmentinvoice.service.InvoiceSettingsService;

/**
 * Unit tests for InvoiceService focusing on debt calculation and interest logic.
 */
@ExtendWith(MockitoExtension.class)
class InvoiceServiceUnitTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private InvoiceSettingsService settingsService;

    @InjectMocks
    private InvoiceService invoiceService;

    private Room testRoom;
    private Tenant testTenant;
    private InvoiceSettings testSettings;

    @BeforeEach
    void setUp() {
        // Create test room and tenant
        testRoom = new Room();
        testRoom.setId(1L);
        testRoom.setNumber(201);

        testTenant = new Tenant();
        testTenant.setId(1L);
        testTenant.setName("Test Tenant");

        // Create default invoice settings with 2% monthly interest
        testSettings = new InvoiceSettings();
        testSettings.setId(1L);
        testSettings.setInterestRatePerMonth(new BigDecimal("2.00"));
    }

    @Test
    void testCalculateAccumulatedDebt_noUnpaidInvoices_returnsZero() {
        // Arrange
        Long roomId = 1L;
        Integer billingYear = 2025;
        Integer billingMonth = 1;

        when(invoiceRepository.findUnpaidInvoicesByRoomBeforePeriod(
            roomId, billingYear, billingMonth, Status.PAID))
            .thenReturn(Collections.emptyList());

        // Act
        DebtCalculation result = invoiceService.calculateAccumulatedDebt(roomId, billingYear, billingMonth);

        // Assert
        assertEquals(BigDecimal.ZERO, result.getPreviousBalance());
        assertEquals(BigDecimal.ZERO, result.getInterestCharge());
        assertEquals(BigDecimal.ZERO, result.getAccumulatedTotal());

        verify(invoiceRepository).findUnpaidInvoicesByRoomBeforePeriod(roomId, billingYear, billingMonth, Status.PAID);
    }

    @Test
    void testCalculateAccumulatedDebt_withSingleUnpaidInvoice_noInterest() {
        // Arrange: Invoice from current month (just issued, not overdue yet)
        Long roomId = 1L;
        Integer billingYear = 2025;
        Integer billingMonth = 2;

        Invoice unpaidInvoice = createInvoice(1L, 2025, 1, new BigDecimal("5000.00"), Status.PENDING);
        unpaidInvoice.setDueDate(LocalDate.of(2025, 2, 5)); // Due date same month as new invoice

        when(invoiceRepository.findUnpaidInvoicesByRoomBeforePeriod(
            roomId, billingYear, billingMonth, Status.PAID))
            .thenReturn(Collections.singletonList(unpaidInvoice));
        when(settingsService.getSettings()).thenReturn(testSettings);

        // Act
        DebtCalculation result = invoiceService.calculateAccumulatedDebt(roomId, billingYear, billingMonth);

        // Assert
        assertEquals(0, new BigDecimal("5000.00").compareTo(result.getPreviousBalance()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getInterestCharge())); // No interest yet (0 months overdue)
        assertEquals(0, new BigDecimal("5000.00").compareTo(result.getAccumulatedTotal()));
    }

    @Test
    void testCalculateAccumulatedDebt_withOverdueInvoice_calculatesInterest() {
        // Arrange: Invoice 2 months overdue
        Long roomId = 1L;
        Integer billingYear = 2025;
        Integer billingMonth = 4; // Creating invoice for April 2025

        // Unpaid invoice from December 2024, due January 5, 2025
        Invoice overdueInvoice = createInvoice(1L, 2024, 12, new BigDecimal("3000.00"), Status.PENDING);
        overdueInvoice.setDueDate(LocalDate.of(2025, 1, 5)); // Due in January

        // New invoice date: April 1, 2025
        // Months between Jan 5 and Apr 1 = 2 complete months (Jan 5 -> Feb 5 -> Mar 5 -> Apr 1)
        // Interest = 3000 * 2% * 2 = 120.00

        when(invoiceRepository.findUnpaidInvoicesByRoomBeforePeriod(
            roomId, billingYear, billingMonth, Status.PAID))
            .thenReturn(Collections.singletonList(overdueInvoice));
        when(settingsService.getSettings()).thenReturn(testSettings);

        // Act
        DebtCalculation result = invoiceService.calculateAccumulatedDebt(roomId, billingYear, billingMonth);

        // Assert
        assertEquals(0, new BigDecimal("3000.00").compareTo(result.getPreviousBalance()));
        assertEquals(0, new BigDecimal("120.00").compareTo(result.getInterestCharge())); // 3000 * 0.02 * 2
        assertEquals(0, new BigDecimal("3120.00").compareTo(result.getAccumulatedTotal())); // 3000 + 120
    }

    @Test
    void testCalculateAccumulatedDebt_withMultipleUnpaidInvoices_sumsCorrectly() {
        // Arrange: Multiple unpaid invoices with different amounts
        Long roomId = 1L;
        Integer billingYear = 2025;
        Integer billingMonth = 6; // June 2025

        Invoice invoice1 = createInvoice(1L, 2024, 11, new BigDecimal("2000.00"), Status.PENDING);
        invoice1.setDueDate(LocalDate.of(2024, 12, 5)); // Due Dec 5, billing Jun 1 = 5 complete months

        Invoice invoice2 = createInvoice(2L, 2024, 12, new BigDecimal("2500.00"), Status.PENDING);
        invoice2.setDueDate(LocalDate.of(2025, 1, 5)); // Due Jan 5, billing Jun 1 = 4 complete months

        Invoice invoice3 = createInvoice(3L, 2025, 1, new BigDecimal("3000.00"), Status.PENDING);
        invoice3.setDueDate(LocalDate.of(2025, 2, 5)); // Due Feb 5, billing Jun 1 = 3 complete months

        // Interest calculations (complete months):
        // invoice1: 2000 * 0.02 * 5 = 200.00
        // invoice2: 2500 * 0.02 * 4 = 200.00
        // invoice3: 3000 * 0.02 * 3 = 180.00
        // Total interest = 580.00

        when(invoiceRepository.findUnpaidInvoicesByRoomBeforePeriod(
            roomId, billingYear, billingMonth, Status.PAID))
            .thenReturn(Arrays.asList(invoice1, invoice2, invoice3));
        when(settingsService.getSettings()).thenReturn(testSettings);

        // Act
        DebtCalculation result = invoiceService.calculateAccumulatedDebt(roomId, billingYear, billingMonth);

        // Assert
        assertEquals(0, new BigDecimal("7500.00").compareTo(result.getPreviousBalance())); // 2000 + 2500 + 3000
        assertEquals(0, new BigDecimal("580.00").compareTo(result.getInterestCharge())); // 200 + 200 + 180
        assertEquals(0, new BigDecimal("8080.00").compareTo(result.getAccumulatedTotal())); // 7500 + 580
    }

    @Test
    void testCalculateAccumulatedDebt_withNullInterestRate_noInterest() {
        // Arrange
        Long roomId = 1L;
        Integer billingYear = 2025;
        Integer billingMonth = 2;

        Invoice unpaidInvoice = createInvoice(1L, 2024, 12, new BigDecimal("4000.00"), Status.PENDING);
        unpaidInvoice.setDueDate(LocalDate.of(2025, 1, 5)); // 1 month overdue

        InvoiceSettings settingsWithNullRate = new InvoiceSettings();
        settingsWithNullRate.setInterestRatePerMonth(null); // Null interest rate

        when(invoiceRepository.findUnpaidInvoicesByRoomBeforePeriod(
            roomId, billingYear, billingMonth, Status.PAID))
            .thenReturn(Collections.singletonList(unpaidInvoice));
        when(settingsService.getSettings()).thenReturn(settingsWithNullRate);

        // Act
        DebtCalculation result = invoiceService.calculateAccumulatedDebt(roomId, billingYear, billingMonth);

        // Assert
        assertEquals(0, new BigDecimal("4000.00").compareTo(result.getPreviousBalance()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getInterestCharge())); // No interest due to null rate
        assertEquals(0, new BigDecimal("4000.00").compareTo(result.getAccumulatedTotal()));
    }

    @Test
    void testCalculateAccumulatedDebt_withZeroInterestRate_noInterest() {
        // Arrange
        Long roomId = 1L;
        Integer billingYear = 2025;
        Integer billingMonth = 2;

        Invoice unpaidInvoice = createInvoice(1L, 2024, 12, new BigDecimal("5000.00"), Status.OVERDUE);
        unpaidInvoice.setDueDate(LocalDate.of(2025, 1, 5));

        InvoiceSettings settingsWithZeroRate = new InvoiceSettings();
        settingsWithZeroRate.setInterestRatePerMonth(BigDecimal.ZERO);

        when(invoiceRepository.findUnpaidInvoicesByRoomBeforePeriod(
            roomId, billingYear, billingMonth, Status.PAID))
            .thenReturn(Collections.singletonList(unpaidInvoice));
        when(settingsService.getSettings()).thenReturn(settingsWithZeroRate);

        // Act
        DebtCalculation result = invoiceService.calculateAccumulatedDebt(roomId, billingYear, billingMonth);

        // Assert
        assertEquals(0, new BigDecimal("5000.00").compareTo(result.getPreviousBalance()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getInterestCharge()));
        assertEquals(0, new BigDecimal("5000.00").compareTo(result.getAccumulatedTotal()));
    }

    @Test
    void testCalculateAccumulatedDebt_withNullTotalBaht_treatsAsZero() {
        // Arrange
        Long roomId = 1L;
        Integer billingYear = 2025;
        Integer billingMonth = 2;

        Invoice invoiceWithNullTotal = createInvoice(1L, 2024, 12, null, Status.PENDING);
        invoiceWithNullTotal.setDueDate(LocalDate.of(2025, 1, 5));

        when(invoiceRepository.findUnpaidInvoicesByRoomBeforePeriod(
            roomId, billingYear, billingMonth, Status.PAID))
            .thenReturn(Collections.singletonList(invoiceWithNullTotal));
        when(settingsService.getSettings()).thenReturn(testSettings);

        // Act
        DebtCalculation result = invoiceService.calculateAccumulatedDebt(roomId, billingYear, billingMonth);

        // Assert
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getPreviousBalance()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getInterestCharge()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getAccumulatedTotal()));
    }

    @Test
    void testCalculateAccumulatedDebt_withNullDueDate_noInterestCalculated() {
        // Arrange
        Long roomId = 1L;
        Integer billingYear = 2025;
        Integer billingMonth = 2;

        Invoice invoiceWithNullDueDate = createInvoice(1L, 2024, 12, new BigDecimal("3000.00"), Status.PENDING);
        invoiceWithNullDueDate.setDueDate(null); // No due date

        when(invoiceRepository.findUnpaidInvoicesByRoomBeforePeriod(
            roomId, billingYear, billingMonth, Status.PAID))
            .thenReturn(Collections.singletonList(invoiceWithNullDueDate));
        when(settingsService.getSettings()).thenReturn(testSettings);

        // Act
        DebtCalculation result = invoiceService.calculateAccumulatedDebt(roomId, billingYear, billingMonth);

        // Assert
        assertEquals(0, new BigDecimal("3000.00").compareTo(result.getPreviousBalance()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getInterestCharge())); // No interest without due date
        assertEquals(0, new BigDecimal("3000.00").compareTo(result.getAccumulatedTotal()));
    }

    @Test
    void testCalculateAccumulatedDebt_excludesCurrentPeriod() {
        // Arrange: Make sure invoices from the same period are NOT included
        Long roomId = 1L;
        Integer billingYear = 2025;
        Integer billingMonth = 3;

        // This should be handled by repository query, but verify service doesn't break
        // Repository should only return invoices BEFORE March 2025
        when(invoiceRepository.findUnpaidInvoicesByRoomBeforePeriod(
            roomId, billingYear, billingMonth, Status.PAID))
            .thenReturn(Collections.emptyList()); // Repository correctly excludes current period

        // Act
        DebtCalculation result = invoiceService.calculateAccumulatedDebt(roomId, billingYear, billingMonth);

        // Assert
        assertEquals(BigDecimal.ZERO, result.getAccumulatedTotal());
        verify(invoiceRepository).findUnpaidInvoicesByRoomBeforePeriod(roomId, billingYear, billingMonth, Status.PAID);
    }

    @Test
    void testGetInvoicesForCurrentMonth_returnsCurrentMonthInvoices() {
        // Arrange
        LocalDate now = LocalDate.now();
        List<Invoice> expectedInvoices = Arrays.asList(
            createInvoice(1L, now.getYear(), now.getMonthValue(), new BigDecimal("5000.00"), Status.PENDING),
            createInvoice(2L, now.getYear(), now.getMonthValue(), new BigDecimal("6000.00"), Status.PAID)
        );

        when(invoiceRepository.findByBillingYearAndBillingMonth(now.getYear(), now.getMonthValue()))
            .thenReturn(expectedInvoices);

        // Act
        List<Invoice> result = invoiceService.getInvoicesForCurrentMonth();

        // Assert
        assertEquals(2, result.size());
        verify(invoiceRepository).findByBillingYearAndBillingMonth(now.getYear(), now.getMonthValue());
    }

    @Test
    void testDebtCalculation_objectCreation() {
        // Test DebtCalculation DTO
        BigDecimal previousBalance = new BigDecimal("1000.00");
        BigDecimal interestCharge = new BigDecimal("50.00");
        BigDecimal accumulatedTotal = new BigDecimal("1050.00");

        DebtCalculation debtCalc = new DebtCalculation(previousBalance, interestCharge, accumulatedTotal);

        assertEquals(previousBalance, debtCalc.getPreviousBalance());
        assertEquals(interestCharge, debtCalc.getInterestCharge());
        assertEquals(accumulatedTotal, debtCalc.getAccumulatedTotal());
    }

    // Helper method to create test invoices
    private Invoice createInvoice(Long id, Integer year, Integer month, BigDecimal total, Status status) {
        Invoice invoice = new Invoice();
        invoice.setId(id);
        invoice.setRoom(testRoom);
        invoice.setTenant(testTenant);
        invoice.setBillingYear(year);
        invoice.setBillingMonth(month);
        invoice.setTotalBaht(total);
        invoice.setStatus(status);
        invoice.setIssueDate(LocalDate.of(year, month, 1));
        return invoice;
    }
}
