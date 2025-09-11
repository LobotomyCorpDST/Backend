package com.devsop.project.apartmentinvoice.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.devsop.project.apartmentinvoice.entity.Room;
import com.devsop.project.apartmentinvoice.repository.RoomRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;

    public Room getById(Long id) {
        return roomRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Room id " + id + " not found"));
    }

    public Room getByNumber(Integer number) {
        return roomRepository.findByNumber(number)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Room number " + number + " not found"));
    }
}
