package com.devsop.project.apartmentinvoice.unit.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
import org.springframework.web.server.ResponseStatusException;

import com.devsop.project.apartmentinvoice.controller.RoomController;
import com.devsop.project.apartmentinvoice.controller.RoomController.RoomView;
import com.devsop.project.apartmentinvoice.entity.Lease;
import com.devsop.project.apartmentinvoice.entity.Lease.Status;
import com.devsop.project.apartmentinvoice.entity.Room;
import com.devsop.project.apartmentinvoice.entity.Tenant;
import com.devsop.project.apartmentinvoice.repository.LeaseRepository;
import com.devsop.project.apartmentinvoice.repository.RoomRepository;

/**
 * Unit tests for RoomController covering CRUD operations and search functionality.
 */
@ExtendWith(MockitoExtension.class)
class RoomControllerUnitTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private LeaseRepository leaseRepository;

    @InjectMocks
    private RoomController roomController;

    private Room testRoom;
    private Tenant testTenant;
    private Lease testLease;

    @BeforeEach
    void setUp() {
        // Setup test room
        testRoom = new Room();
        testRoom.setId(1L);
        testRoom.setNumber(201);
        testRoom.setStatus("OCCUPIED");

        // Setup test tenant
        testTenant = new Tenant();
        testTenant.setId(1L);
        testTenant.setName("John Doe");

        // Setup test lease
        testLease = new Lease();
        testLease.setId(1L);
        testLease.setRoom(testRoom);
        testLease.setTenant(testTenant);
        testLease.setStatus(Status.ACTIVE);
    }

    // ==================== GET ALL ROOMS TESTS ====================

    @Test
    void testGetAllRooms_noSearch_returnsAllRooms() {
        // Arrange
        Room room2 = new Room();
        room2.setId(2L);
        room2.setNumber(202);
        room2.setStatus("FREE");

        when(roomRepository.findAll()).thenReturn(Arrays.asList(testRoom, room2));
        when(leaseRepository.findByRoom_IdAndStatus(1L, Status.ACTIVE))
                .thenReturn(Collections.singletonList(testLease));
        when(leaseRepository.findByRoom_IdAndStatus(2L, Status.ACTIVE))
                .thenReturn(Collections.emptyList());

        // Act
        List<RoomView> result = roomController.all(null);

        // Assert
        assertEquals(2, result.size());
        assertEquals(201, result.get(0).getNumber());
        assertEquals("OCCUPIED", result.get(0).getStatus());
        assertEquals(true, result.get(0).getIsOwned());
        assertEquals("John Doe", result.get(0).getTenantName());

        assertEquals(202, result.get(1).getNumber());
        assertEquals("FREE", result.get(1).getStatus());
        assertEquals(false, result.get(1).getIsOwned());
        assertNull(result.get(1).getTenantName());

        verify(roomRepository).findAll();
    }

    @Test
    void testGetAllRooms_withRoomNumberSearch_returnsMatchingRooms() {
        // Arrange
        when(roomRepository.findAll()).thenReturn(Collections.singletonList(testRoom));
        when(leaseRepository.findByRoom_IdAndStatus(1L, Status.ACTIVE))
                .thenReturn(Collections.singletonList(testLease));

        // Act
        List<RoomView> result = roomController.all("201");

        // Assert
        assertEquals(1, result.size());
        assertEquals(201, result.get(0).getNumber());
        verify(roomRepository).findAll();
    }

    @Test
    void testGetAllRooms_withTenantNameSearch_returnsMatchingRooms() {
        // Arrange
        when(roomRepository.findAll()).thenReturn(Collections.singletonList(testRoom));
        when(leaseRepository.findByRoom_IdAndStatus(1L, Status.ACTIVE))
                .thenReturn(Collections.singletonList(testLease));

        // Act
        List<RoomView> result = roomController.all("John");

        // Assert
        assertEquals(1, result.size());
        assertEquals("John Doe", result.get(0).getTenantName());
        verify(roomRepository).findAll();
    }

    @Test
    void testGetAllRooms_withNonMatchingSearch_returnsEmpty() {
        // Arrange
        when(roomRepository.findAll()).thenReturn(Collections.singletonList(testRoom));
        when(leaseRepository.findByRoom_IdAndStatus(1L, Status.ACTIVE))
                .thenReturn(Collections.singletonList(testLease));

        // Act
        List<RoomView> result = roomController.all("999");

        // Assert
        assertEquals(0, result.size());
        verify(roomRepository).findAll();
    }

    // ==================== CREATE ROOM TESTS ====================

    @Test
    void testCreateRoom_validData_createsRoom() {
        // Arrange
        Room newRoom = new Room();
        newRoom.setNumber(301);

        when(roomRepository.existsByNumber(301)).thenReturn(false);
        when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> {
            Room saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        // Act
        Room result = roomController.create(newRoom);

        // Assert
        assertNotNull(result);
        assertEquals(301, result.getNumber());
        assertEquals("FREE", result.getStatus()); // Default status
        verify(roomRepository).existsByNumber(301);
        verify(roomRepository).save(any(Room.class));
    }

    @Test
    void testCreateRoom_nullNumber_throwsBadRequest() {
        // Arrange
        Room invalidRoom = new Room();
        invalidRoom.setNumber(null);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            roomController.create(invalidRoom);
        });

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("number is required"));
        verify(roomRepository, never()).save(any());
    }

    @Test
    void testCreateRoom_duplicateNumber_throwsConflict() {
        // Arrange
        Room duplicateRoom = new Room();
        duplicateRoom.setNumber(201);

        when(roomRepository.existsByNumber(201)).thenReturn(true);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            roomController.create(duplicateRoom);
        });

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertTrue(exception.getReason().contains("already exists"));
        verify(roomRepository).existsByNumber(201);
        verify(roomRepository, never()).save(any());
    }

    // ==================== GET ROOM BY NUMBER TESTS ====================

    @Test
    void testGetRoomByNumber_validNumber_returnsRoomView() {
        // Arrange
        when(roomRepository.findByNumber(201)).thenReturn(Optional.of(testRoom));
        when(leaseRepository.findByRoom_IdAndStatus(1L, Status.ACTIVE))
                .thenReturn(Collections.singletonList(testLease));

        // Act
        RoomView result = roomController.byNumber(201);

        // Assert
        assertNotNull(result);
        assertEquals(201, result.getNumber());
        assertEquals("OCCUPIED", result.getStatus());
        assertEquals(true, result.getIsOwned());
        assertEquals("John Doe", result.getTenantName());
        verify(roomRepository).findByNumber(201);
    }

    @Test
    void testGetRoomByNumber_notFound_throwsNotFoundException() {
        // Arrange
        when(roomRepository.findByNumber(999)).thenReturn(Optional.empty());

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            roomController.byNumber(999);
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(roomRepository).findByNumber(999);
    }

    // ==================== GET ROOM BY ID TESTS ====================

    @Test
    void testGetRoomById_validId_returnsRoom() {
        // Arrange
        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));

        // Act
        Room result = roomController.byId(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(201, result.getNumber());
        verify(roomRepository).findById(1L);
    }

    @Test
    void testGetRoomById_notFound_throwsNotFoundException() {
        // Arrange
        when(roomRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            roomController.byId(999L);
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Room not found"));
        verify(roomRepository).findById(999L);
    }

    // ==================== UPDATE ROOM TESTS ====================

    @Test
    void testUpdateRoom_changeNumber_updatesSuccessfully() {
        // Arrange
        Room patch = new Room();
        patch.setNumber(301);

        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(roomRepository.existsByNumber(301)).thenReturn(false);
        when(roomRepository.save(any(Room.class))).thenReturn(testRoom);

        // Act
        Room result = roomController.update(1L, patch);

        // Assert
        assertNotNull(result);
        verify(roomRepository).findById(1L);
        verify(roomRepository).existsByNumber(301);
        verify(roomRepository).save(argThat(room -> room.getNumber().equals(301)));
    }

    @Test
    void testUpdateRoom_changeNumberToDuplicate_throwsConflict() {
        // Arrange
        Room patch = new Room();
        patch.setNumber(202); // Different number that already exists

        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(roomRepository.existsByNumber(202)).thenReturn(true);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            roomController.update(1L, patch);
        });

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertTrue(exception.getReason().contains("already exists"));
        verify(roomRepository).findById(1L);
        verify(roomRepository).existsByNumber(202);
        verify(roomRepository, never()).save(any());
    }

    @Test
    void testUpdateRoom_changeStatus_updatesSuccessfully() {
        // Arrange
        Room patch = new Room();
        patch.setStatus("MAINTENANCE");

        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(roomRepository.save(any(Room.class))).thenReturn(testRoom);

        // Act
        Room result = roomController.update(1L, patch);

        // Assert
        assertNotNull(result);
        verify(roomRepository).save(argThat(room -> room.getStatus().equals("MAINTENANCE")));
    }

    @Test
    void testUpdateRoom_roomNotFound_throwsNotFoundException() {
        // Arrange
        Room patch = new Room();
        patch.setNumber(301);

        when(roomRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            roomController.update(999L, patch);
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(roomRepository).findById(999L);
        verify(roomRepository, never()).save(any());
    }

    // ==================== DELETE ROOM TESTS ====================

    @Test
    void testDeleteRoom_validId_deletesSuccessfully() {
        // Arrange
        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));

        // Act
        roomController.delete(1L);

        // Assert
        verify(roomRepository).findById(1L);
        verify(roomRepository).delete(testRoom);
    }

    @Test
    void testDeleteRoom_notFound_throwsNotFoundException() {
        // Arrange
        when(roomRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            roomController.delete(999L);
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(roomRepository).findById(999L);
        verify(roomRepository, never()).delete(any());
    }

    // ==================== PING TEST ====================

    @Test
    void testPing_returnsOk() {
        // Act
        String result = roomController.ping();

        // Assert
        assertEquals("ok", result);
    }
}
