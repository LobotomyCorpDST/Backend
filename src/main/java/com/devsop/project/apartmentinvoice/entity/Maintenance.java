package com.devsop.project.apartmentinvoice.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Maintenance {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @NotNull
  private Room room;

  @Column(length = 500, nullable = false)
  @NotBlank
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length=20)
  private Status status = Status.PLANNED;   // PLANNED, IN_PROGRESS, COMPLETED, CANCELED

  @NotNull
  private LocalDate scheduledDate;          // วันที่นัดซ่อม

  private LocalDate completedDate;          // วันที่ทำเสร็จ

  @Column(precision = 12, scale = 2)
  private BigDecimal costBaht;              // ค่าใช้จ่าย

  @Column(length = 200)
  private String responsiblePerson;         // ชื่อผู้รับผิดชอบ

  @Column(length = 50)
  private String responsiblePhone;          // เบอร์โทรศัพท์ผู้รับผิดชอบ

  public enum Status {
    PLANNED,
    IN_PROGRESS,
    COMPLETED,
    CANCELED
  }
}
