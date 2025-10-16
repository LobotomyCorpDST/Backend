package com.devsop.project.apartmentinvoice.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.devsop.project.apartmentinvoice.entity.Tenant;
import com.devsop.project.apartmentinvoice.repository.TenantRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
public class TenantController {
  private final TenantRepository repo;

  @GetMapping public List<Tenant> all() { return repo.findAll(); }

  @PostMapping public Tenant create(@Valid @RequestBody Tenant t) { return repo.save(t); }

  @PutMapping("/{id}")
  public Tenant update(@PathVariable Long id, @Valid @RequestBody Tenant t) {
    t.setId(id);
    return repo.save(t);
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable Long id) { repo.deleteById(id); }

//  @GetMapping("/{id}")
//  public Tenant findById(@PathVariable Long id) {
//      return repo.findById(id)
//              .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
//  }
}