package com.devsop.project.apartmentinvoice.service;

import com.devsop.project.apartmentinvoice.entity.Room;
import com.devsop.project.apartmentinvoice.repository.RoomRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RoomService_UnitTest {

    @Mock
    private RoomRepository roomRepository;

    @InjectMocks
    private RoomService roomService;

    @Test
    @DisplayName("getById should return Room when found")
    void whenGetById_withValidId_thenReturnRoom() {
        long roomId = 1L;
        Room room = new Room();
        room.setId(roomId);
        room.setNumber(101);

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));

        Room foundRoom = roomService.getById(roomId);

        assertThat(foundRoom).isNotNull();
        assertThat(foundRoom.getNumber()).isEqualTo(101);
    }

    @Test
    @DisplayName("getById should throw exception when not found")
    void whenGetById_withInvalidId_thenThrowException() {
        long roomId = 99L;
        when(roomRepository.findById(roomId)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> {
            roomService.getById(roomId);
        });
    }

    @Test
    @DisplayName("getByNumber should return Room when found")
    void whenGetByNumber_withValidNumber_thenReturnRoom() {
        int roomNumber = 101;
        Room room = new Room();
        room.setId(1L);
        room.setNumber(roomNumber);

        when(roomRepository.findByNumber(roomNumber)).thenReturn(Optional.of(room));

        Room foundRoom = roomService.getByNumber(roomNumber);

        assertThat(foundRoom).isNotNull();
        assertThat(foundRoom.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getByNumber should throw exception when not found")
    void whenGetByNumber_withInvalidNumber_thenThrowException() {
        int roomNumber = 999;
        when(roomRepository.findByNumber(roomNumber)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> {
            roomService.getByNumber(roomNumber);
        });
    }
}
