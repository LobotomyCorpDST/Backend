package com.devsop.project.apartmentinvoice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.devsop.project.apartmentinvoice.entity.UserAccount;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
  Optional<UserAccount> findByUsername(String username);

  /**
   * Find all users with room relationship eagerly loaded
   */
  @Query("SELECT u FROM UserAccount u LEFT JOIN FETCH u.room")
  List<UserAccount> findAllWithRoom();

  /**
   * Find user by ID with room relationship eagerly loaded
   */
  @Query("SELECT u FROM UserAccount u LEFT JOIN FETCH u.room WHERE u.id = :id")
  Optional<UserAccount> findByIdWithRoom(@Param("id") Long id);
}
