package com.devsop.project.apartmentinvoice.unit.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import com.devsop.project.apartmentinvoice.controller.LeaseController;
import com.devsop.project.apartmentinvoice.controller.LeaseController.CreateLeaseRequest;
import com.devsop.project.apartmentinvoice.controller.LeaseController.EndLeaseRequest;
import com.devsop.project.apartmentinvoice.controller.LeaseController.LeaseView;
import com.devsop.project.apartmentinvoice.dto.BulkPrintRequest;
import com.devsop.project.apartmentinvoice.entity.Lease;
import com.devsop.project.apartmentinvoice.entity.Lease.Status;
import com.devsop.project.apartmentinvoice.entity.Room;
import com.devsop.project.apartmentinvoice.entity.Tenant;
import com.devsop.project.apartmentinvoice.repository.LeaseRepository;
import com.devsop.project.apartmentinvoice.repository.RoomRepository;
import com.devsop.project.apartmentinvoice.service.LeaseService;
import com.devsop.project.apartmentinvoice.service.PdfService;

/**
 * Unit tests for LeaseController covering lease CRUD operations, queries, and PDF generation.
 */
@ExtendWith(MockitoExtension.class)
class LeaseControllerUnitTest {

    @Mock
    private LeaseRepository leaseRepository;

    @Mock
    private LeaseService leaseService;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private PdfService pdfService;

    @InjectMocks
    private LeaseController leaseController;

    private Lease testLease;
    private Room testRoom;
    private Tenant testTenant;

    @BeforeEach
    void setUp() {
        testRoom = new Room();
        testRoom.setId(1L);
        testRoom.setNumber(201);
        testRoom.setStatus("OCCUPIED");

        testTenant = new Tenant();
        testTenant.setId(1L);
        testTenant.setName("John Doe");
        testTenant.setPhone("0812345678");

        testLease = new Lease();
        testLease.setId(1L);
        testLease.setRoom(testRoom);
        testLease.setTenant(testTenant);
        testLease.setStatus(Status.ACTIVE);
        testLease.setStartDate(LocalDate.of(2025, 1, 1));
        testLease.setMonthlyRent(new BigDecimal("5000.00"));
        testLease.setDepositBaht(new BigDecimal("10000.00"));
    }

    // ==================== GET ALL LEASES TESTS ====================

    @Test
    void testGetAllLeases_noFilters_returnsAllLeases() {
        // Arrange
        when(leaseRepository.findAllWithRefs()).thenReturn(Collections.singletonList(testLease));

        // Act
        List<LeaseView> result = leaseController.all(null, null);

        // Assert
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(Status.ACTIVE, result.get(0).getStatus());
        verify(leaseRepository).findAllWithRefs();
    }

    @Test
    void testGetAllLeases_withStatusFilter_returnsFiltered() {
        // Arrange
        Lease endedLease = new Lease();
        endedLease.setId(2L);
        endedLease.setStatus(Status.ENDED);
        endedLease.setRoom(testRoom);
        endedLease.setTenant(testTenant);

        when(leaseRepository.findAllWithRefs()).thenReturn(Arrays.asList(testLease, endedLease));

        // Act
        List<LeaseView> result = leaseController.all(Status.ACTIVE, null);

        // Assert
        assertEquals(1, result.size());
        assertEquals(Status.ACTIVE, result.get(0).getStatus());
    }

    @Test
    void testGetAllLeases_withSearch_returnsMatching() {
        // Arrange
        when(leaseRepository.findAllWithRefs()).thenReturn(Collections.singletonList(testLease));

        // Act - search by tenant name
        List<LeaseView> result = leaseController.all(null, "John");

        // Assert
        assertEquals(1, result.size());
        assertEquals("John Doe", result.get(0).getTenant().getName());
    }

    // ==================== GET LEASE BY ID TESTS ====================

    @Test
    void testGetLeaseById_validId_returnsLease() {
        // Arrange
        when(leaseRepository.findByIdWithRefs(1L)).thenReturn(Optional.of(testLease));

        // Act
        LeaseView result = leaseController.get(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(leaseRepository).findByIdWithRefs(1L);
    }

    // ==================== GET LEASES BY ROOM TESTS ====================

    @Test
    void testGetLeasesByRoom_returnsAllRoomLeases() {
        // Arrange
        when(leaseRepository.findByRoomIdWithRefs(1L)).thenReturn(Collections.singletonList(testLease));

        // Act
        List<LeaseView> result = leaseController.byRoom(1L, null);

        // Assert
        assertEquals(1, result.size());
        verify(leaseRepository).findByRoomIdWithRefs(1L);
    }

    @Test
    void testGetLeasesByRoom_activeOnly_returnsActiveOnly() {
        // Arrange
        when(leaseRepository.findByRoomIdWithRefs(1L)).thenReturn(Collections.singletonList(testLease));

        // Act
        List<LeaseView> result = leaseController.byRoom(1L, true);

        // Assert
        assertEquals(1, result.size());
        assertEquals(Status.ACTIVE, result.get(0).getStatus());
    }

    // ==================== GET ACTIVE LEASE BY ROOM NUMBER TESTS ====================

    @Test
    void testGetActiveByRoomNumber_found_returnsLease() {
        // Arrange
        when(leaseRepository.findFirstActiveByRoomNumber(201)).thenReturn(Optional.of(testLease));

        // Act
        ResponseEntity<LeaseView> result = leaseController.getActiveByRoomNumber(201);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(1L, result.getBody().getId());
    }

    @Test
    void testGetActiveByRoomNumber_notFound_returnsNoContent() {
        // Arrange
        when(leaseRepository.findFirstActiveByRoomNumber(999)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<LeaseView> result = leaseController.getActiveByRoomNumber(999);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        assertNull(result.getBody());
    }

    // ==================== CREATE LEASE TESTS ====================

    @Test
    void testCreateLease_legacyFormat_createsLease() {
        // Arrange
        CreateLeaseRequest request = new CreateLeaseRequest();
        request.setTenantId(1L);
        request.setRoomId(1L);
        request.setStartDate(LocalDate.of(2025, 1, 1));

        when(leaseService.createLease(any(Lease.class))).thenReturn(testLease);

        // Act
        ResponseEntity<LeaseView> result = leaseController.create(request);

        // Assert
        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertNotNull(result.getBody());
        verify(leaseService).createLease(any(Lease.class));
    }

    @Test
    void testCreateLease_withRoomNumber_createsLease() {
        // Arrange
        CreateLeaseRequest request = new CreateLeaseRequest();
        request.setTenantId(1L);
        request.setRoomNumber(201);
        request.setStartDate(LocalDate.of(2025, 1, 1));

        when(roomRepository.findByNumber(201)).thenReturn(Optional.of(testRoom));
        when(leaseService.createLease(any(Lease.class))).thenReturn(testLease);

        // Act
        ResponseEntity<LeaseView> result = leaseController.create(request);

        // Assert
        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        verify(roomRepository).findByNumber(201);
        verify(leaseService).createLease(any(Lease.class));
    }

    @Test
    void testCreateLease_missingFields_throwsBadRequest() {
        // Arrange
        CreateLeaseRequest request = new CreateLeaseRequest();
        request.setTenantId(1L);
        // Missing roomId and startDate

        // Act & Assert
        assertThrows(ResponseStatusException.class, () -> {
            leaseController.create(request);
        });
    }

    // ==================== UPDATE LEASE TESTS ====================

    @Test
    void testUpdateLease_validData_updatesLease() {
        // Arrange
        Lease patch = new Lease();
        patch.setMonthlyRent(new BigDecimal("6000.00"));

        when(leaseService.updateLease(eq(1L), any(Lease.class))).thenReturn(testLease);

        // Act
        LeaseView result = leaseController.update(1L, patch);

        // Assert
        assertNotNull(result);
        verify(leaseService).updateLease(eq(1L), any(Lease.class));
    }

    // ==================== END LEASE TESTS ====================

    @Test
    void testEndLease_validData_endsLease() {
        // Arrange
        EndLeaseRequest request = new EndLeaseRequest(LocalDate.now());
        testLease.setStatus(Status.ENDED);

        when(leaseService.endLease(1L, request.getEndDate())).thenReturn(testLease);

        // Act
        ResponseEntity<LeaseView> result = leaseController.end(1L, request);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(leaseService).endLease(1L, request.getEndDate());
    }

    // ==================== SETTLE LEASE TESTS ====================

    @Test
    void testSettleLease_marksAsSettled() {
        // Arrange
        LocalDate settleDate = LocalDate.now();
        testLease.setSettled(true);

        when(leaseService.settleLease(1L, settleDate)).thenReturn(testLease);

        // Act
        LeaseView result = leaseController.settle(1L, settleDate);

        // Assert
        assertNotNull(result);
        verify(leaseService).settleLease(1L, settleDate);
    }

    // ==================== DELETE LEASE TESTS ====================

    @Test
    void testDeleteLease_deletesSuccessfully() {
        // Arrange
        doNothing().when(leaseService).deleteLease(1L);

        // Act
        leaseController.delete(1L);

        // Assert
        verify(leaseService).deleteLease(1L);
    }

    // ==================== BULK PDF TESTS ====================

    @Test
    void testBulkPdf_validIds_generatesPdf() throws Exception {
        // Arrange
        BulkPrintRequest request = new BulkPrintRequest();
        request.setIds(Arrays.asList(1L, 2L));

        byte[] pdfBytes1 = new byte[]{1, 2, 3};
        byte[] pdfBytes2 = new byte[]{4, 5, 6};
        byte[] mergedPdf = new byte[]{1, 2, 3, 4, 5, 6};

        when(leaseRepository.findByIdWithRefs(1L)).thenReturn(Optional.of(testLease));
        when(leaseRepository.findByIdWithRefs(2L)).thenReturn(Optional.of(testLease));
        when(pdfService.generateLeasePdf(any(Lease.class))).thenReturn(pdfBytes1).thenReturn(pdfBytes2);
        when(pdfService.mergePdfs(any())).thenReturn(mergedPdf);

        // Act
        ResponseEntity<byte[]> result = leaseController.getBulkLeasePdf(request);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        verify(pdfService, times(2)).generateLeasePdf(any(Lease.class));
        verify(pdfService).mergePdfs(any());
    }

    @Test
    void testBulkPdf_noValidLeases_throwsBadRequest() {
        // Arrange
        BulkPrintRequest request = new BulkPrintRequest();
        request.setIds(Arrays.asList(999L));

        when(leaseRepository.findByIdWithRefs(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResponseStatusException.class, () -> {
            leaseController.getBulkLeasePdf(request);
        });
    }
}
