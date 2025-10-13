package com.devsop.project.apartmentinvoice.service;

import com.devsop.project.apartmentinvoice.entity.UserAccount;
import com.devsop.project.apartmentinvoice.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
  private final UserAccountRepository repo;
  private final PasswordEncoder encoder;

  public UserAccount register(String username, String rawPassword, String role) {
    var user = UserAccount.builder()
        .username(username)
        .password(encoder.encode(rawPassword))
        .role(role)
        .build();
    return repo.save(user);
  }

  public Optional<UserAccount> findByUsername(String username) {
    return repo.findByUsername(username);
  }
}
