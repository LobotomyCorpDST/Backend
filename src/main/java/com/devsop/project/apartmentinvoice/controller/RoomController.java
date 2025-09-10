package com.devsop.project.apartmentinvoice.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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
  public Room create(@RequestBody Room r) {
    if (r.getNumber() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "number is required");
    if (repo.existsByNumber(r.getNumber())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Room number already exists");
    if (r.getStatus() == null) r.setStatus("FREE");
    return repo.save(r);
  }

  @GetMapping("/by-number/{number}")
  public Room byNumber(@PathVariable Integer number) {
    return repo.findByNumber(number).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  @GetMapping("/ping")
  public String ping() { return "ok"; }
}
