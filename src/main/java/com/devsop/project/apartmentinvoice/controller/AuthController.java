package com.devsop.project.apartmentinvoice.controller;

import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devsop.project.apartmentinvoice.entity.UserAccount;
import com.devsop.project.apartmentinvoice.repository.UserAccountRepository;
import com.devsop.project.apartmentinvoice.security.JwtTokenProvider;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final UserAccountRepository userRepo;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;

  // POST /api/auth/register
  @PostMapping("/register")
  public ResponseEntity<?> register(@RequestBody LoginRequest req) {
    Optional<UserAccount> exists = userRepo.findByUsername(req.getUsername());
    if (exists.isPresent()) {
      return ResponseEntity.badRequest().body(Map.of("error", "username_already_exists"));
    }

    UserAccount user = new UserAccount();
    user.setUsername(req.getUsername());
    user.setPassword(passwordEncoder.encode(req.getPassword()));
    user.setRole("STAFF"); // หรือ "ADMIN" ตามที่ต้องการ

    UserAccount created = userRepo.save(user);
    return ResponseEntity.ok(new SimpleUser(created.getUsername(), created.getRole()));
  }

  // POST /api/auth/login
  @PostMapping("/login")
  public ResponseEntity<?> login(@RequestBody LoginRequest req) {
    Optional<UserAccount> userOpt = userRepo.findByUsername(req.getUsername());
    if (userOpt.isEmpty()) {
      return ResponseEntity.status(401).body(new ErrorMsg("Invalid credentials"));
    }
    UserAccount user = userOpt.get();
    if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
      return ResponseEntity.status(401).body(new ErrorMsg("Invalid credentials"));
    }

    // สำคัญ: เรียก generate(String username) เพื่อเลี่ยงชนิดไม่ตรง
    String token = jwtTokenProvider.generate(user.getUsername(), user.getRole());
    return ResponseEntity.ok(new LoginResponse(token, user.getUsername()));
  }

  /* ===== DTOs แบบง่าย ===== */

  @Data
  public static class LoginRequest {
    private String username;
    private String password;
  }

  @Data
  public static class LoginResponse {
    private final String token;
    private final String username;
  }

  @Data
  public static class SimpleUser {
    private final String username;
    private final String role;
  }

  @Data
  public static class ErrorMsg {
    private final String error;
  }
}
