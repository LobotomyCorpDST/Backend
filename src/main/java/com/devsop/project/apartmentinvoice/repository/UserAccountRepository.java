package com.devsop.project.apartmentinvoice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.devsop.project.apartmentinvoice.entity.UserAccount;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
  Optional<UserAccount> findByUsername(String username);
}
