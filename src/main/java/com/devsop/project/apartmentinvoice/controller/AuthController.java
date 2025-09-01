package com.devsop.project.apartmentinvoice.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.Data;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  @PostMapping("/login")
  public LoginResponse login(@RequestBody LoginRequest req) {
  
    LoginResponse res = new LoginResponse();
    res.setToken("mock-token-123");
    res.setUsername(req.getUsername());
    return res;
  }

  @Data
  public static class LoginRequest {
    private String username;
    private String password;
  }

  @Data
  public static class LoginResponse {
    private String token;
    private String username;
  }
}
