package com.devsop.project.apartmentinvoice.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Lease {

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @NotNull
  @ToString.Exclude
  private Room room;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @NotNull
  @ToString.Exclude
  private Tenant tenant;

  @NotNull
  private LocalDate startDate;

  private LocalDate endDate;                 // กำหนดเมื่อสิ้นสุดสัญญา

  @Column(precision = 12, scale = 2)
  private BigDecimal monthlyRent;            // ค่าเช่าต่อเดือน

  @Column(precision = 12, scale = 2)
  private BigDecimal depositBaht;            // เงินมัดจำ (ถ้ามี)

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Status status = Status.ACTIVE;     // ACTIVE, ENDED

  @Column(length = 1000)
  private String notes;

  public enum Status { ACTIVE, ENDED }
}
