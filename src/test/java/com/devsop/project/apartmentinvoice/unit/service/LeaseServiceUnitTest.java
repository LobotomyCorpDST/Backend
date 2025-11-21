package com.devsop.project.apartmentinvoice.unit.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.devsop.project.apartmentinvoice.entity.Lease;
import com.devsop.project.apartmentinvoice.entity.Lease.Status;
import com.devsop.project.apartmentinvoice.entity.Room;
import com.devsop.project.apartmentinvoice.entity.Tenant;
import com.devsop.project.apartmentinvoice.repository.LeaseRepository;
import com.devsop.project.apartmentinvoice.repository.RoomRepository;
import com.devsop.project.apartmentinvoice.repository.TenantRepository;
import com.devsop.project.apartmentinvoice.service.LeaseService;

/**
 * Comprehensive unit tests for LeaseService covering lease lifecycle management.
 */
@ExtendWith(MockitoExtension.class)
class LeaseServiceUnitTest {

    @Mock
    private LeaseRepository leaseRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private LeaseService leaseService;

    private Room testRoom;
    private Tenant testTenant;
    private Lease testLease;

    @BeforeEach
    void setUp() {
        // Setup test room
        testRoom = new Room();
        testRoom.setId(1L);
        testRoom.setNumber(201);
        testRoom.setStatus("FREE");
        testRoom.setCommonFeeBaht(new BigDecimal("100.00"));
        testRoom.setGarbageFeeBaht(new BigDecimal("50.00"));

        // Setup test tenant
        testTenant = new Tenant();
        testTenant.setId(1L);
        testTenant.setName("John Doe");
        testTenant.setPhone("0812345678");

        // Setup test lease
        testLease = new Lease();
        testLease.setId(1L);
        testLease.setRoom(testRoom);
        testLease.setTenant(testTenant);
        testLease.setStartDate(LocalDate.now());
        testLease.setMonthlyRent(new BigDecimal("5000.00"));
        testLease.setDepositBaht(new BigDecimal("10000.00"));
        testLease.setStatus(Status.ACTIVE);
    }

    // ==================== CREATE LEASE TESTS ====================

    @Test
    void testCreateLease_validData_createsLeaseAndUpdatesRoom() {
        // Arrange
        Long tenantId = 1L;
        Long roomId = 1L;
        LocalDate startDate = LocalDate.now();

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(testTenant));
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(roomRepository.findByNumber(201)).thenReturn(Optional.of(testRoom)); // Called by createLease(draft)
        when(leaseRepository.findByRoom_IdAndStatus(roomId, Status.ACTIVE))
                .thenReturn(Collections.emptyList());
        when(roomRepository.save(any(Room.class))).thenReturn(testRoom);
        when(leaseRepository.save(any(Lease.class))).thenAnswer(invocation -> {
            Lease saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // Act
        Lease result = leaseService.createLease(tenantId, roomId, startDate);

        // Assert
        assertNotNull(result);

        verify(tenantRepository, atLeast(1)).findById(tenantId); // Called twice (createLease methods)
        verify(roomRepository).findById(roomId);
        verify(roomRepository).save(any(Room.class)); // Room status updated
        verify(leaseRepository).save(any(Lease.class));
    }

    @Test
    void testCreateLease_tenantNotFound_throwsNotFoundException() {
        // Arrange
        Long tenantId = 999L;
        Long roomId = 1L;
        LocalDate startDate = LocalDate.now();

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            leaseService.createLease(tenantId, roomId, startDate);
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Tenant"));
        verify(tenantRepository).findById(tenantId);
        verify(leaseRepository, never()).save(any());
    }

    @Test
    void testCreateLease_roomNotFound_throwsNotFoundException() {
        // Arrange
        Long tenantId = 1L;
        Long roomId = 999L;
        LocalDate startDate = LocalDate.now();

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(testTenant));
        when(roomRepository.findById(roomId)).thenReturn(Optional.empty());

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            leaseService.createLease(tenantId, roomId, startDate);
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Room"));
        verify(roomRepository).findById(roomId);
        verify(leaseRepository, never()).save(any());
    }

    @Test
    void testCreateLease_roomAlreadyHasActiveLease_throwsConflictException() {
        // Arrange
        Long tenantId = 1L;
        Long roomId = 1L;
        LocalDate startDate = LocalDate.now();

        Lease existingLease = new Lease();
        existingLease.setId(2L);
        existingLease.setStatus(Status.ACTIVE);

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(testTenant));
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(leaseRepository.findByRoom_IdAndStatus(roomId, Status.ACTIVE))
                .thenReturn(Collections.singletonList(existingLease));

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            leaseService.createLease(tenantId, roomId, startDate);
        });

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertTrue(exception.getReason().contains("ACTIVE lease"));
        verify(leaseRepository, never()).save(any());
    }

    @Test
    void testCreateLeaseByRoomNumber_validData_createsLease() {
        // Arrange
        Long tenantId = 1L;
        Integer roomNumber = 201;
        LocalDate startDate = LocalDate.now();

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(testTenant));
        when(roomRepository.findByNumber(roomNumber)).thenReturn(Optional.of(testRoom));
        when(leaseRepository.findByRoom_IdAndStatus(testRoom.getId(), Status.ACTIVE))
                .thenReturn(Collections.emptyList());
        when(roomRepository.save(any(Room.class))).thenReturn(testRoom);
        when(leaseRepository.save(any(Lease.class))).thenAnswer(invocation -> {
            Lease saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // Act
        Lease result = leaseService.createLeaseByRoomNumber(tenantId, roomNumber, startDate);

        // Assert
        assertNotNull(result);
        verify(roomRepository, atLeast(1)).findByNumber(roomNumber); // Called twice (createLeaseByRoomNumber + createLease)
        verify(leaseRepository).save(any(Lease.class));
    }

    @Test
    void testCreateLease_nullStartDate_usesCurrentDate() {
        // Arrange
        Long tenantId = 1L;
        Long roomId = 1L;
        LocalDate nullStartDate = null;

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(testTenant));
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(roomRepository.findByNumber(201)).thenReturn(Optional.of(testRoom)); // Called by createLease(draft)
        when(leaseRepository.findByRoom_IdAndStatus(roomId, Status.ACTIVE))
                .thenReturn(Collections.emptyList());
        when(roomRepository.save(any(Room.class))).thenReturn(testRoom);
        when(leaseRepository.save(any(Lease.class))).thenAnswer(invocation -> {
            Lease saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // Act
        Lease result = leaseService.createLease(tenantId, roomId, nullStartDate);

        // Assert
        assertNotNull(result);
        verify(leaseRepository).save(any(Lease.class));
    }

    // ==================== UPDATE LEASE TESTS ====================

    @Test
    void testUpdateLease_validPatch_updatesFields() {
        // Arrange
        Long leaseId = 1L;
        Lease patch = new Lease();
        patch.setMonthlyRent(new BigDecimal("6000.00"));
        patch.setEndDate(LocalDate.now().plusYears(1));
        patch.setNotes("Updated notes");

        when(leaseRepository.findById(leaseId)).thenReturn(Optional.of(testLease));
        when(leaseRepository.save(any(Lease.class))).thenReturn(testLease);

        // Act
        Lease result = leaseService.updateLease(leaseId, patch);

        // Assert
        assertNotNull(result);
        verify(leaseRepository).findById(leaseId);
        verify(leaseRepository).save(argThat(lease ->
            lease.getMonthlyRent().compareTo(new BigDecimal("6000.00")) == 0 &&
            lease.getNotes().equals("Updated notes")
        ));
    }

    @Test
    void testUpdateLease_changeTenant_syncsTenantToRoom() {
        // Arrange
        Long leaseId = 1L;
        Tenant newTenant = new Tenant();
        newTenant.setId(2L);
        newTenant.setName("Jane Smith");

        Lease patch = new Lease();
        patch.setTenant(newTenant);

        when(leaseRepository.findById(leaseId)).thenReturn(Optional.of(testLease));
        when(tenantRepository.findById(2L)).thenReturn(Optional.of(newTenant));
        when(leaseRepository.save(any(Lease.class))).thenReturn(testLease);

        // Act
        Lease result = leaseService.updateLease(leaseId, patch);

        // Assert
        assertNotNull(result);
        verify(tenantRepository).findById(2L);
        verify(roomRepository, atLeastOnce()).save(any(Room.class)); // Room tenant updated
    }

    @Test
    void testUpdateLease_changeRoom_freesOldRoomAndOccupiesNew() {
        // Arrange
        Long leaseId = 1L;
        Room newRoom = new Room();
        newRoom.setId(2L);
        newRoom.setNumber(202);
        newRoom.setStatus("FREE");

        Lease patch = new Lease();
        patch.setRoom(newRoom);

        when(leaseRepository.findById(leaseId)).thenReturn(Optional.of(testLease));
        when(roomRepository.findByNumber(202)).thenReturn(Optional.of(newRoom)); // Service looks up by number
        when(roomRepository.save(any(Room.class))).thenReturn(testRoom);
        when(leaseRepository.save(any(Lease.class))).thenReturn(testLease);

        // Act
        Lease result = leaseService.updateLease(leaseId, patch);

        // Assert
        assertNotNull(result);
        verify(roomRepository).findByNumber(202);
        // Verify rooms are saved (both old and new)
        verify(roomRepository, atLeast(2)).save(any(Room.class));
    }

    @Test
    void testUpdateLease_leaseNotFound_throwsNotFoundException() {
        // Arrange
        Long leaseId = 999L;
        Lease patch = new Lease();

        when(leaseRepository.findById(leaseId)).thenReturn(Optional.empty());

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            leaseService.updateLease(leaseId, patch);
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(leaseRepository, never()).save(any());
    }

    // ==================== END LEASE TESTS ====================

    @Test
    void testEndLease_validId_endsLeaseAndFreesRoom() {
        // Arrange
        Long leaseId = 1L;
        LocalDate endDate = LocalDate.now();

        when(leaseRepository.findById(leaseId)).thenReturn(Optional.of(testLease));
        when(leaseRepository.save(any(Lease.class))).thenReturn(testLease);

        // Act
        Lease result = leaseService.endLease(leaseId, endDate);

        // Assert
        assertNotNull(result);
        verify(leaseRepository).save(argThat(lease ->
            Status.ENDED.equals(lease.getStatus()) &&
            endDate.equals(lease.getEndDate())
        ));
        verify(roomRepository).save(argThat(room ->
            "FREE".equals(room.getStatus()) &&
            room.getTenant() == null
        ));
    }

    @Test
    void testEndLease_nullEndDate_usesCurrentDate() {
        // Arrange
        Long leaseId = 1L;
        LocalDate nullEndDate = null;

        when(leaseRepository.findById(leaseId)).thenReturn(Optional.of(testLease));
        when(leaseRepository.save(any(Lease.class))).thenReturn(testLease);

        // Act
        Lease result = leaseService.endLease(leaseId, nullEndDate);

        // Assert
        assertNotNull(result);
        verify(leaseRepository).save(argThat(lease ->
            lease.getEndDate() != null &&
            lease.getEndDate().isEqual(LocalDate.now())
        ));
    }

    // ==================== SETTLE LEASE TESTS ====================

    @Test
    void testSettleLease_validId_marksAsSettled() {
        // Arrange
        Long leaseId = 1L;
        LocalDate settleDate = LocalDate.now();

        when(leaseRepository.findById(leaseId)).thenReturn(Optional.of(testLease));
        when(leaseRepository.save(any(Lease.class))).thenReturn(testLease);

        // Act
        Lease result = leaseService.settleLease(leaseId, settleDate);

        // Assert
        assertNotNull(result);
        verify(leaseRepository).save(argThat(lease ->
            Boolean.TRUE.equals(lease.getSettled()) &&
            settleDate.equals(lease.getSettledDate())
        ));
    }

    // ==================== DELETE LEASE TESTS ====================

    @Test
    void testDeleteLease_validId_deletesAndFreesRoom() {
        // Arrange
        Long leaseId = 1L;

        when(leaseRepository.findById(leaseId)).thenReturn(Optional.of(testLease));
        when(roomRepository.findById(testRoom.getId())).thenReturn(Optional.of(testRoom));

        // Act
        leaseService.deleteLease(leaseId);

        // Assert
        verify(roomRepository).save(argThat(room ->
            "FREE".equals(room.getStatus()) &&
            room.getTenant() == null
        ));
        verify(leaseRepository).delete(testLease);
    }

    @Test
    void testDeleteLease_leaseNotFound_throwsNotFoundException() {
        // Arrange
        Long leaseId = 999L;

        when(leaseRepository.findById(leaseId)).thenReturn(Optional.empty());

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            leaseService.deleteLease(leaseId);
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(leaseRepository, never()).delete(any());
    }
}
