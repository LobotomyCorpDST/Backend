package com.devsop.project.apartmentinvoice.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devsop.project.apartmentinvoice.entity.UserAccount;
import com.devsop.project.apartmentinvoice.service.UserAccountService;

import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')") // Only ADMIN can access this controller
public class UserAccountController {

    private final UserAccountService userAccountService;

    /**
     * Get all users (ADMIN only)
     */
    @GetMapping
    public ResponseEntity<List<UserAccountResponse>> getAllUsers() {
        List<UserAccount> users = userAccountService.getAllUsers();
        List<UserAccountResponse> response = users.stream()
                .map(u -> new UserAccountResponse(
                    u.getId(),
                    u.getUsername(),
                    u.getRole(),
                    u.getRoomId(),
                    u.getRoom() != null ? u.getRoom().getNumber() : null,
                    u.getRoomIds()))
                .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * Get user by ID (ADMIN only)
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserAccountResponse> getUserById(@PathVariable Long id) {
        return userAccountService.getUserById(id)
                .map(u -> ResponseEntity.ok(
                        new UserAccountResponse(
                            u.getId(),
                            u.getUsername(),
                            u.getRole(),
                            u.getRoomId(),
                            u.getRoom() != null ? u.getRoom().getNumber() : null,
                            u.getRoomIds())))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create new user (ADMIN only)
     */
    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequest request) {
        try {
            UserAccount created = userAccountService.createUser(
                    request.getUsername(),
                    request.getPassword(),
                    request.getRole(),
                    request.getRoomNumber(),
                    request.getRoomNumbers());

            UserAccountResponse response = new UserAccountResponse(
                    created.getId(),
                    created.getUsername(),
                    created.getRole(),
                    created.getRoomId(),
                    created.getRoom() != null ? created.getRoom().getNumber() : null,
                    created.getRoomIds());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Update user (username, role, roomNumber) - ADMIN only
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        try {
            UserAccount updated = userAccountService.updateUser(
                    id,
                    request.getUsername(),
                    request.getRole(),
                    request.getRoomNumber(),
                    request.getRoomNumbers());

            UserAccountResponse response = new UserAccountResponse(
                    updated.getId(),
                    updated.getUsername(),
                    updated.getRole(),
                    updated.getRoomId(),
                    updated.getRoom() != null ? updated.getRoom().getNumber() : null,
                    updated.getRoomIds());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Change user password - ADMIN only
     */
    @PatchMapping("/{id}/change-password")
    public ResponseEntity<?> changePassword(@PathVariable Long id,
            @Valid @RequestBody ChangePasswordRequest request) {
        try {
            userAccountService.changePassword(id, request.getNewPassword());
            return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete user - ADMIN only
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            userAccountService.deleteUser(id);
            return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /* ===== DTOs ===== */

    @Data
    public static class CreateUserRequest {
        private String username;
        private String password;
        private String role; // ADMIN, STAFF, USER
        private Integer roomNumber; // Room number (e.g., 201, 305) - only for USER role (deprecated)
        private String roomNumbers; // Comma-separated room numbers (e.g., "201, 305, 412") - new field
    }

    @Data
    public static class UpdateUserRequest {
        private String username;
        private String role;
        private Integer roomNumber; // Room number (e.g., 201, 305) (deprecated)
        private String roomNumbers; // Comma-separated room numbers (e.g., "201, 305, 412") - new field
    }

    @Data
    public static class ChangePasswordRequest {
        private String newPassword;
    }

    @Data
    public static class UserAccountResponse {
        private final Long id;
        private final String username;
        private final String role;
        private final Long roomId; // Deprecated - for backward compatibility
        private final Integer roomNumber; // Single room number (deprecated) - for backward compatibility
        private final String roomNumbers; // Comma-separated room numbers (e.g., "201, 305, 412")
    }
}
