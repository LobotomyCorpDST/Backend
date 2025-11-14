package com.devsop.project.apartmentinvoice.controller;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ApiInfoController {

  @GetMapping({ "", "/" })
  public ResponseEntity<Map<String, Object>> root() {
    Map<String, Object> payload = Map.of(
        "service", "apartment-invoice-backend",
        "status", "ok",
        "timestamp", Instant.now().toString());
    return ResponseEntity.ok(payload);
  }
}
