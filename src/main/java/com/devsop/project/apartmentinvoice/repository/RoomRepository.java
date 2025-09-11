package com.devsop.project.apartmentinvoice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.devsop.project.apartmentinvoice.entity.Room;

public interface RoomRepository extends JpaRepository<Room, Long> {
    Optional<Room> findByNumber(Integer number);
    boolean existsByNumber(Integer number);
}
