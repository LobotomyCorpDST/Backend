package com.devsop.project.apartmentinvoice.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Invoice {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false) @NotNull
  private Room room;

  @ManyToOne(optional = false) @NotNull
  private Tenant tenant;

  @Column(nullable = false)
  private Integer billingYear;

  @Column(nullable = false)
  private Integer billingMonth;

  @Column(nullable = false)
  private LocalDate issueDate;

  @Column(nullable = false)
  private LocalDate dueDate;

  @Column(precision = 12, scale = 2)
  private BigDecimal rentBaht;

  @Column(precision = 12, scale = 2)
  private BigDecimal electricityUnits;

  @Column(precision = 12, scale = 2)
  private BigDecimal electricityRate;

  @Column(precision = 12, scale = 2)
  private BigDecimal electricityBaht;

  @Column(precision = 12, scale = 2)
  private BigDecimal waterUnits;

  @Column(precision = 12, scale = 2)
  private BigDecimal waterRate;

  @Column(precision = 12, scale = 2)
  private BigDecimal waterBaht;

  @Column(precision = 12, scale = 2)
  private BigDecimal otherBaht;

  @Column(precision = 12, scale = 2)
  private BigDecimal commonFeeBaht;

  @Column(precision = 12, scale = 2)
  private BigDecimal garbageFeeBaht;

  @Column(precision = 12, scale = 2)
  private BigDecimal totalBaht;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Status status = Status.PENDING;

  public enum Status { PENDING, PAID, OVERDUE }
}
