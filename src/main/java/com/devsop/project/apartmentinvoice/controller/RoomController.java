package com.devsop.project.apartmentinvoice.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devsop.project.apartmentinvoice.entity.Room;
import com.devsop.project.apartmentinvoice.repository.RoomRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {
  private final RoomRepository repo;

  @GetMapping
  public List<Room> all() { return repo.findAll(); }

  @PostMapping
  public Room create(@RequestBody Room r) { return repo.save(r); }

  @GetMapping("/ping")
  public String ping() { return "ok"; }
}
