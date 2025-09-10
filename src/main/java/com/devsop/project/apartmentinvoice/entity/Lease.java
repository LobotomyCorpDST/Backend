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

  // ---------- Custom printable fields ----------
  @Column(length = 200)
  private String customName;                 // ชื่อผู้เช่า (custom กรอกเอง)

  @Column(length = 32)
  private String customIdCard;               // บัตรประชาชน (custom)

  @Column(columnDefinition = "TEXT")
  private String customAddress;              // ที่อยู่ (custom)

  @Column(columnDefinition = "TEXT")
  private String customRules;                // กฎ/รายละเอียดพิเศษ (custom)

  // ---------- Settled flag ----------
  private Boolean settled = Boolean.FALSE;   // default false
  private LocalDate settledDate;             // วันที่เคลียร์สัญญาเรียบร้อย

  public enum Status { ACTIVE, ENDED }
}
