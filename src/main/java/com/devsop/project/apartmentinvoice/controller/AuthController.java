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

  // --- REGISTER ---
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

  // --- LOGIN ---
  @PostMapping("/login")
  public ResponseEntity<?> login(@RequestBody LoginRequest req) {

    // ✅ STEP 2: Allow static guest login (no DB needed)
    if ("guest".equalsIgnoreCase(req.getUsername()) && "guest123".equals(req.getPassword())) {
      String token = jwtTokenProvider.generate("guest", "GUEST");
      return ResponseEntity.ok(new LoginResponse(token, "guest", "GUEST", null, null));
    }

    // ✅ Default: normal user login via DB
    Optional<UserAccount> userOpt = userRepo.findByUsername(req.getUsername());
    if (userOpt.isEmpty()) {
      return ResponseEntity.status(401).body(new ErrorMsg("Invalid credentials"));
    }
    UserAccount user = userOpt.get();
    if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
      return ResponseEntity.status(401).body(new ErrorMsg("Invalid credentials"));
    }

    String token = jwtTokenProvider.generate(user.getUsername(), user.getRole());
    return ResponseEntity.ok(new LoginResponse(token, user.getUsername(), user.getRole(), user.getRoomId(), user.getRoomIds()));
  }

  /* ===== DTOs ===== */

  @Data
  public static class LoginRequest {
    private String username;
    private String password;
  }

  @Data
  public static class LoginResponse {
    private final String token;
    private final String username;
    private final String role;
    private final Long roomId; // Deprecated - for backward compatibility
    private final String roomNumbers; // Comma-separated room numbers (e.g., "201, 305, 412")
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
