package com.devsop.project.apartmentinvoice.controller;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ApiInfoController {

  @RequestMapping(value = { "", "/" }, method = {
      RequestMethod.GET,
      RequestMethod.POST,
      RequestMethod.HEAD,
      RequestMethod.OPTIONS
  })
  public ResponseEntity<Map<String, Object>> root() {
    Map<String, Object> payload = Map.of(
        "service", "apartment-invoice-backend",
        "status", "ok",
        "timestamp", Instant.now().toString());
    return ResponseEntity.ok(payload);
  }
}
