package com.devsop.project.apartmentinvoice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_account")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class UserAccount {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String username;

  @Column(nullable = false)
  private String password;

  @Column(nullable = false)
  private String role; // ADMIN, STAFF, USER, GUEST

  /**
   * Room ID for USER role (nullable - only used for USER role)
   * Links a USER account to their specific room for access control
   */
  @Column(name = "room_id")
  private Long roomId;
}
