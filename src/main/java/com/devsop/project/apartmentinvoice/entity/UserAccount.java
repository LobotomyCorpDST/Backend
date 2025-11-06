package com.devsop.project.apartmentinvoice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
   * Kept for backward compatibility
   */
  @Column(name = "room_id", insertable = false, updatable = false)
  private Long roomId;

  /**
   * Room relationship for USER role (nullable - only used for USER role)
   * Lazy loaded to avoid unnecessary queries for ADMIN/STAFF/GUEST
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "room_id")
  private Room room;

  /**
   * Multiple room numbers for USER role (comma-separated, e.g., "201,305,412")
   * New field to support multiple room assignments
   */
  @Column(name = "room_ids", length = 500)
  private String roomIds;
}
