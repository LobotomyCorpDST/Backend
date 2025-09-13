package com.devsop.project.apartmentinvoice.service;

import com.devsop.project.apartmentinvoice.entity.Lease;
import com.devsop.project.apartmentinvoice.entity.Room;
import com.devsop.project.apartmentinvoice.repository.LeaseRepository;
import com.devsop.project.apartmentinvoice.repository.RoomRepository;
import com.devsop.project.apartmentinvoice.repository.TenantRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LeaseService_UnitTest {

    @Mock
    private LeaseRepository leaseRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private LeaseService leaseService;

    @Test
    @DisplayName("endLease should update lease and room status correctly")
    void whenEndLease_thenLeaseAndRoomAreUpdated() {
        long leaseId = 1L;
        long roomId = 10L;

        Room testRoom = new Room();
        testRoom.setId(roomId);
        testRoom.setStatus("OCCUPIED");

        Lease testLease = Lease.builder()
                .id(leaseId)
                .room(testRoom)
                .status(Lease.Status.ACTIVE)
                .build();

        when(leaseRepository.findById(leaseId)).thenReturn(Optional.of(testLease));

        LocalDate endDate = LocalDate.now();
        leaseService.endLease(leaseId, endDate);

        assertThat(testLease.getStatus()).isEqualTo(Lease.Status.ENDED);
        assertThat(testLease.getEndDate()).isEqualTo(endDate);
        assertThat(testRoom.getStatus()).isEqualTo("FREE");
        assertThat(testRoom.getTenant()).isNull();

        verify(leaseRepository).save(testLease);
        verify(roomRepository).save(testRoom);
    }

    @Test
    @DisplayName("endLease should throw exception if lease not found")
    void whenEndLeaseWithInvalidId_thenThrowNotFoundException() {
        long nonExistentLeaseId = 99L;
        when(leaseRepository.findById(nonExistentLeaseId)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            leaseService.endLease(nonExistentLeaseId, LocalDate.now());
        });

        assertThat(exception.getStatusCode().value()).isEqualTo(404);
        assertThat(exception.getReason()).contains("Lease id 99 not found");
    }
}
