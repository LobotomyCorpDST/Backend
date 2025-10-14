package com.devsop.project.apartmentinvoice.common;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.devsop.project.apartmentinvoice.service.TenantService;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {
    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult().getFieldErrors()
        .forEach(fe -> errors.put(fe.getField(), fe.getDefaultMessage()));
    return ResponseEntity.badRequest().body(Map.of(
        "message", "validation_error",
        "errors", errors
    ));
  }

  @ExceptionHandler(TenantService.DuplicateResourceException.class)
  public ResponseEntity<?> handleDup(TenantService.DuplicateResourceException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", ex.getMessage()));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<?> handleConstraint(ConstraintViolationException ex) {
    return ResponseEntity.badRequest().body(Map.of("message", "constraint_violation"));
  }
}
