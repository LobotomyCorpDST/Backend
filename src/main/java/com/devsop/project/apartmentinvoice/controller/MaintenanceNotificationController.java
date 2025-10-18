package com.devsop.project.apartmentinvoice.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.devsop.project.apartmentinvoice.dto.MaintenanceDueDto;
import com.devsop.project.apartmentinvoice.repository.MaintenanceRepository;

@RestController
@RequestMapping("/api/notifications")
public class MaintenanceNotificationController {

    private final MaintenanceRepository repo;

    public MaintenanceNotificationController(MaintenanceRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/maintenance")
    public ResponseEntity<List<MaintenanceDueDto>> getDueToday(
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        LocalDate target = (date != null) ? date : LocalDate.now();
        var result = repo.findDueOn(target);
        return ResponseEntity.ok(result);
    }
}
