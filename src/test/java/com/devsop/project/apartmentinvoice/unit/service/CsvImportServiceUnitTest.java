package com.devsop.project.apartmentinvoice.unit.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.devsop.project.apartmentinvoice.entity.Invoice;
import com.devsop.project.apartmentinvoice.entity.Lease;
import com.devsop.project.apartmentinvoice.entity.Room;
import com.devsop.project.apartmentinvoice.entity.Tenant;
import com.devsop.project.apartmentinvoice.metrics.InvoiceMetrics;
import com.devsop.project.apartmentinvoice.repository.InvoiceRepository;
import com.devsop.project.apartmentinvoice.repository.LeaseRepository;
import com.devsop.project.apartmentinvoice.repository.MaintenanceRepository;
import com.devsop.project.apartmentinvoice.repository.RoomRepository;
import com.devsop.project.apartmentinvoice.service.CsvImportService;
import com.devsop.project.apartmentinvoice.service.CsvImportService.ImportResult;
import com.devsop.project.apartmentinvoice.service.InvoiceService;
import com.devsop.project.apartmentinvoice.service.InvoiceService.DebtCalculation;
import com.devsop.project.apartmentinvoice.service.storage.StorageService;

/**
 * Unit tests for CsvImportService focusing on CSV parsing, validation, and invoice creation.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CsvImportServiceUnitTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private LeaseRepository leaseRepository;

    @Mock
    private MaintenanceRepository maintenanceRepository;

    @Mock
    private InvoiceService invoiceService;

    @Mock
    private InvoiceMetrics invoiceMetrics;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private CsvImportService csvImportService;

    private Room testRoom;
    private Tenant testTenant;
    private Lease testLease;

    @BeforeEach
    void setUp() {
        // Create test room
        testRoom = new Room();
        testRoom.setId(1L);
        testRoom.setNumber(201);
        testRoom.setCommonFeeBaht(new BigDecimal("100.00"));
        testRoom.setGarbageFeeBaht(new BigDecimal("50.00"));

        // Create test tenant
        testTenant = new Tenant();
        testTenant.setId(1L);
        testTenant.setName("Test Tenant");

        // Create test lease
        testLease = new Lease();
        testLease.setId(1L);
        testLease.setRoom(testRoom);
        testLease.setTenant(testTenant);
        testLease.setMonthlyRent(new BigDecimal("5000.00"));
        testLease.setStartDate(LocalDate.of(2024, 1, 1));
        testLease.setEndDate(LocalDate.of(2025, 12, 31));

        testRoom.setTenant(testTenant);

        // Setup metrics to just call the supplier
        when(invoiceMetrics.recordImport(any())).thenAnswer(invocation -> {
            java.util.function.Supplier<?> supplier = invocation.getArgument(0);
            return supplier.get();
        });
    }

    @Test
    void testImportCsv_emptyFile_throwsException() {
        // Arrange
        MultipartFile emptyFile = createMockCsvFile("", "test.csv");

        // Act & Assert
        assertThrows(ResponseStatusException.class, () -> {
            csvImportService.importInvoicesFromCsv(emptyFile);
        });

        verify(invoiceMetrics).incrementImportErrors();
    }

    @Test
    void testImportCsv_nonCsvFile_throwsException() {
        // Arrange
        MultipartFile txtFile = createMockCsvFile("data", "test.txt");

        // Act & Assert
        assertThrows(ResponseStatusException.class, () -> {
            csvImportService.importInvoicesFromCsv(txtFile);
        });

        verify(invoiceMetrics).incrementImportErrors();
    }

    @Test
    void testImportCsv_validSingleRow_createsInvoice() throws IOException {
        // Arrange
        String csvContent = "Room Number,Electricity Units,Water Units,Billing Month,Billing Year,Electricity Rate,Water Rate\n" +
                            "201,100,50,1,2025,5.50,8.00";

        MultipartFile csvFile = createMockCsvFile(csvContent, "test.csv");

        when(roomRepository.findByNumber(201)).thenReturn(Optional.of(testRoom));
        when(invoiceRepository.findFirstByRoom_IdAndBillingYearAndBillingMonth(1L, 2025, 1))
                .thenReturn(Optional.empty());
        when(leaseRepository.findActiveLeaseByRoomOnDate(eq(1L), any(LocalDate.class)))
                .thenReturn(Optional.of(testLease));
        when(maintenanceRepository.findByRoom_IdAndStatusAndCompletedDateBetween(anyLong(), any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(invoiceService.calculateAccumulatedDebt(1L, 2025, 1))
                .thenReturn(new DebtCalculation(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

        // Act
        ImportResult result = csvImportService.importInvoicesFromCsv(csvFile);

        // Assert
        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        verify(invoiceRepository, times(1)).save(any(Invoice.class));
        verify(invoiceMetrics).incrementInvoiceCreated();
    }

    @Test
    void testImportCsv_invalidFormat_recordsError() throws IOException {
        // Arrange: CSV with only 5 columns instead of 7
        String csvContent = "Room Number,Electricity Units,Water Units,Billing Month,Billing Year\n" +
                            "201,100,50,1,2025";

        MultipartFile csvFile = createMockCsvFile(csvContent, "test.csv");

        // Act
        ImportResult result = csvImportService.importInvoicesFromCsv(csvFile);

        // Assert
        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertTrue(result.getErrors().get(0).contains("Invalid CSV format"));
        verify(invoiceRepository, never()).save(any());
        verify(invoiceMetrics).incrementImportErrors();
    }

    @Test
    void testImportCsv_invalidNumberFormat_recordsError() throws IOException {
        // Arrange
        String csvContent = "Room Number,Electricity Units,Water Units,Billing Month,Billing Year,Electricity Rate,Water Rate\n" +
                            "ABC,100,50,1,2025,5.50,8.00";

        MultipartFile csvFile = createMockCsvFile(csvContent, "test.csv");

        // Act
        ImportResult result = csvImportService.importInvoicesFromCsv(csvFile);

        // Assert
        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertTrue(result.getErrors().get(0).contains("Invalid number format"));
        verify(invoiceMetrics).incrementImportErrors();
    }

    @Test
    void testImportCsv_invalidBillingMonth_recordsError() throws IOException {
        // Arrange: Billing month 13 (invalid)
        String csvContent = "Room Number,Electricity Units,Water Units,Billing Month,Billing Year,Electricity Rate,Water Rate\n" +
                            "201,100,50,13,2025,5.50,8.00";

        MultipartFile csvFile = createMockCsvFile(csvContent, "test.csv");

        when(roomRepository.findByNumber(201)).thenReturn(Optional.of(testRoom));

        // Act
        ImportResult result = csvImportService.importInvoicesFromCsv(csvFile);

        // Assert
        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertTrue(result.getErrors().get(0).contains("Invalid billing month"));
        verify(invoiceMetrics).incrementImportErrors();
    }

    @Test
    void testImportCsv_roomNotFound_recordsError() throws IOException {
        // Arrange
        String csvContent = "Room Number,Electricity Units,Water Units,Billing Month,Billing Year,Electricity Rate,Water Rate\n" +
                            "999,100,50,1,2025,5.50,8.00";

        MultipartFile csvFile = createMockCsvFile(csvContent, "test.csv");

        when(roomRepository.findByNumber(999)).thenReturn(Optional.empty());

        // Act
        ImportResult result = csvImportService.importInvoicesFromCsv(csvFile);

        // Assert
        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertTrue(result.getErrors().get(0).contains("Room not found"));
        verify(invoiceMetrics).incrementImportErrors();
    }

    @Test
    void testImportCsv_duplicateInvoice_recordsError() throws IOException {
        // Arrange
        String csvContent = "Room Number,Electricity Units,Water Units,Billing Month,Billing Year,Electricity Rate,Water Rate\n" +
                            "201,100,50,1,2025,5.50,8.00";

        MultipartFile csvFile = createMockCsvFile(csvContent, "test.csv");

        Invoice existingInvoice = new Invoice();
        existingInvoice.setId(1L);

        when(roomRepository.findByNumber(201)).thenReturn(Optional.of(testRoom));
        when(invoiceRepository.findFirstByRoom_IdAndBillingYearAndBillingMonth(1L, 2025, 1))
                .thenReturn(Optional.of(existingInvoice)); // Invoice already exists

        // Act
        ImportResult result = csvImportService.importInvoicesFromCsv(csvFile);

        // Assert
        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertTrue(result.getErrors().get(0).contains("Invoice already exists"));
        verify(invoiceRepository, never()).save(any());
        verify(invoiceMetrics).incrementImportErrors();
    }

    @Test
    void testImportCsv_noTenantAssigned_recordsError() throws IOException {
        // Arrange
        String csvContent = "Room Number,Electricity Units,Water Units,Billing Month,Billing Year,Electricity Rate,Water Rate\n" +
                            "201,100,50,1,2025,5.50,8.00";

        MultipartFile csvFile = createMockCsvFile(csvContent, "test.csv");

        Room roomWithoutTenant = new Room();
        roomWithoutTenant.setId(1L);
        roomWithoutTenant.setNumber(201);
        roomWithoutTenant.setTenant(null); // No tenant

        when(roomRepository.findByNumber(201)).thenReturn(Optional.of(roomWithoutTenant));
        when(invoiceRepository.findFirstByRoom_IdAndBillingYearAndBillingMonth(1L, 2025, 1))
                .thenReturn(Optional.empty());
        when(leaseRepository.findActiveLeaseByRoomOnDate(eq(1L), any(LocalDate.class)))
                .thenReturn(Optional.empty()); // No lease either

        // Act
        ImportResult result = csvImportService.importInvoicesFromCsv(csvFile);

        // Assert
        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertTrue(result.getErrors().get(0).contains("has no tenant assigned"));
        verify(invoiceMetrics).incrementImportErrors();
    }

    @Test
    void testImportCsv_multipleRows_processesMixed() throws IOException {
        // Arrange: 3 rows - 1 valid, 2 invalid
        String csvContent = "Room Number,Electricity Units,Water Units,Billing Month,Billing Year,Electricity Rate,Water Rate\n" +
                            "201,100,50,1,2025,5.50,8.00\n" +
                            "999,100,50,1,2025,5.50,8.00\n" +  // Room not found
                            "201,100,50,13,2025,5.50,8.00";    // Invalid month

        MultipartFile csvFile = createMockCsvFile(csvContent, "test.csv");

        when(roomRepository.findByNumber(201)).thenReturn(Optional.of(testRoom));
        when(roomRepository.findByNumber(999)).thenReturn(Optional.empty());
        when(invoiceRepository.findFirstByRoom_IdAndBillingYearAndBillingMonth(anyLong(), anyInt(), anyInt()))
                .thenReturn(Optional.empty());
        when(leaseRepository.findActiveLeaseByRoomOnDate(eq(1L), any(LocalDate.class)))
                .thenReturn(Optional.of(testLease));
        when(maintenanceRepository.findByRoom_IdAndStatusAndCompletedDateBetween(anyLong(), any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(invoiceService.calculateAccumulatedDebt(anyLong(), anyInt(), anyInt()))
                .thenReturn(new DebtCalculation(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

        // Act
        ImportResult result = csvImportService.importInvoicesFromCsv(csvFile);

        // Assert
        assertEquals(1, result.getSuccessCount());
        assertEquals(2, result.getFailureCount());
        assertEquals(3, result.getTotalProcessed());
        verify(invoiceRepository, times(1)).save(any(Invoice.class));
    }

    @Test
    void testImportCsv_skipsHeaderRow() throws IOException {
        // Arrange: Header row should be skipped
        String csvContent = "Room Number,Electricity Units,Water Units,Billing Month,Billing Year,Electricity Rate,Water Rate\n" +
                            "201,100,50,1,2025,5.50,8.00";

        MultipartFile csvFile = createMockCsvFile(csvContent, "test.csv");

        when(roomRepository.findByNumber(201)).thenReturn(Optional.of(testRoom));
        when(invoiceRepository.findFirstByRoom_IdAndBillingYearAndBillingMonth(1L, 2025, 1))
                .thenReturn(Optional.empty());
        when(leaseRepository.findActiveLeaseByRoomOnDate(eq(1L), any(LocalDate.class)))
                .thenReturn(Optional.of(testLease));
        when(maintenanceRepository.findByRoom_IdAndStatusAndCompletedDateBetween(anyLong(), any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(invoiceService.calculateAccumulatedDebt(1L, 2025, 1))
                .thenReturn(new DebtCalculation(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

        // Act
        ImportResult result = csvImportService.importInvoicesFromCsv(csvFile);

        // Assert
        assertEquals(1, result.getSuccessCount()); // Only 1 data row processed
        assertEquals(0, result.getFailureCount());
    }

    @Test
    void testImportCsv_skipsEmptyLines() throws IOException {
        // Arrange
        String csvContent = "Room Number,Electricity Units,Water Units,Billing Month,Billing Year,Electricity Rate,Water Rate\n" +
                            "\n" +  // Empty line
                            "201,100,50,1,2025,5.50,8.00\n" +
                            "   \n"; // Whitespace line

        MultipartFile csvFile = createMockCsvFile(csvContent, "test.csv");

        when(roomRepository.findByNumber(201)).thenReturn(Optional.of(testRoom));
        when(invoiceRepository.findFirstByRoom_IdAndBillingYearAndBillingMonth(1L, 2025, 1))
                .thenReturn(Optional.empty());
        when(leaseRepository.findActiveLeaseByRoomOnDate(eq(1L), any(LocalDate.class)))
                .thenReturn(Optional.of(testLease));
        when(maintenanceRepository.findByRoom_IdAndStatusAndCompletedDateBetween(anyLong(), any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(invoiceService.calculateAccumulatedDebt(1L, 2025, 1))
                .thenReturn(new DebtCalculation(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

        // Act
        ImportResult result = csvImportService.importInvoicesFromCsv(csvFile);

        // Assert
        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
    }

    @Test
    void testImportResult_countsCorrectly() {
        // Test ImportResult DTO
        ImportResult result = new ImportResult();

        assertEquals(0, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        assertEquals(0, result.getTotalProcessed());

        result.incrementSuccess();
        result.incrementSuccess();
        result.addError(1, "Error 1");

        assertEquals(2, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertEquals(3, result.getTotalProcessed());

        List<String> errors = result.getErrors();
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("Line 1"));
        assertTrue(errors.get(0).contains("Error 1"));
    }

    // Helper method to create mock CSV file
    private MultipartFile createMockCsvFile(String content, String filename) {
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.isEmpty()).thenReturn(content.isEmpty());
        when(mockFile.getOriginalFilename()).thenReturn(filename);
        when(mockFile.getContentType()).thenReturn("text/csv");

        try {
            InputStream inputStream = new ByteArrayInputStream(content.getBytes());
            when(mockFile.getInputStream()).thenReturn(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return mockFile;
    }
}
