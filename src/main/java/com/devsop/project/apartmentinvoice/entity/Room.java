package com.devsop.project.apartmentinvoice.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Room {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Integer number;
  private String status;

  @ManyToOne
  private Tenant tenant;

  @Column(precision = 12, scale = 2)
  private BigDecimal commonFeeBaht;
  @Column(precision = 12, scale = 2)
  private BigDecimal garbageFeeBaht;
}