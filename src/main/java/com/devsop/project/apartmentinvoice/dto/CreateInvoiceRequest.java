package com.devsop.project.apartmentinvoice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

/**
 * Payload สำหรับสร้างใบแจ้งหนี้
 * หมายเหตุ:
 * - ถ้าไม่ส่ง tenantId ระบบจะดึงจาก Lease ที่ ACTIVE ของห้อง ณ วัน issueDate
 * - ถ้าไม่ส่ง rentBaht ระบบจะดึงจาก monthlyRent ของ Lease
 * - amount (electricityBaht/waterBaht) จะถูกคำนวณจาก units*rate ถ้าไม่ได้ส่ง amount มา
 */
@Data
@JsonInclude(Include.NON_NULL)
public class CreateInvoiceRequest {

  /** ต้องมีเสมอ */
  @NotNull
  private Long roomId;

  /** Optional: ไม่ส่งมาได้ ระบบจะเติมจาก Lease ACTIVE */
  private Long tenantId;

  private Integer billingYear;
  private Integer billingMonth;

  private LocalDate issueDate;
  private LocalDate dueDate;

  @PositiveOrZero
  private BigDecimal rentBaht;

  @PositiveOrZero
  private BigDecimal electricityUnits;
  @PositiveOrZero
  private BigDecimal electricityRate;
  @PositiveOrZero
  private BigDecimal electricityBaht;

  @PositiveOrZero
  private BigDecimal waterUnits;
  @PositiveOrZero
  private BigDecimal waterRate;
  @PositiveOrZero
  private BigDecimal waterBaht;

  @PositiveOrZero
  private BigDecimal otherBaht;

  @PositiveOrZero
  private BigDecimal commonFeeBaht;

  @PositiveOrZero
  private BigDecimal garbageFeeBaht;
}
