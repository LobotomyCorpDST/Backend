package com.devsop.project.apartmentinvoice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.devsop.project.apartmentinvoice.entity.Room;

public interface RoomRepository extends JpaRepository<Room, Long> {}
