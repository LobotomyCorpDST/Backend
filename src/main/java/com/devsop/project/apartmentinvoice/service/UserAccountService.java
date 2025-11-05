package com.devsop.project.apartmentinvoice.service;

import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsop.project.apartmentinvoice.entity.UserAccount;
import com.devsop.project.apartmentinvoice.repository.UserAccountRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserAccountService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Get all user accounts with room relationship eagerly loaded
     */
    public List<UserAccount> getAllUsers() {
        return userAccountRepository.findAllWithRoom();
    }

    /**
     * Get user by ID with room relationship eagerly loaded
     */
    public Optional<UserAccount> getUserById(Long id) {
        return userAccountRepository.findByIdWithRoom(id);
    }

    /**
     * Get user by username
     */
    public Optional<UserAccount> getUserByUsername(String username) {
        return userAccountRepository.findByUsername(username);
    }

    /**
     * Create new user account
     * @throws IllegalArgumentException if username already exists or role is invalid
     */
    @Transactional
    public UserAccount createUser(String username, String password, String role, Long roomId) {
        // Validate username uniqueness
        if (userAccountRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }

        // Validate role
        if (!isValidRole(role)) {
            throw new IllegalArgumentException("Invalid role: " + role + ". Must be one of: ADMIN, STAFF, USER, GUEST");
        }

        // Prevent creating guest via this method (guest is hardcoded)
        if ("GUEST".equalsIgnoreCase(role)) {
            throw new IllegalArgumentException("Cannot create GUEST role via this endpoint. Guest login is handled separately.");
        }

        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role.toUpperCase());
        user.setRoomId(roomId);

        UserAccount saved = userAccountRepository.save(user);
        // Reload with room relationship eagerly loaded
        return userAccountRepository.findByIdWithRoom(saved.getId()).orElse(saved);
    }

    /**
     * Update user account (username and role only)
     * @throws IllegalArgumentException if user not found or trying to delete last admin
     */
    @Transactional
    public UserAccount updateUser(Long id, String username, String role, Long roomId) {
        UserAccount user = userAccountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));

        // Validate role
        if (!isValidRole(role)) {
            throw new IllegalArgumentException("Invalid role: " + role);
        }

        // Prevent changing from ADMIN if this is the last admin
        if ("ADMIN".equalsIgnoreCase(user.getRole()) && !"ADMIN".equalsIgnoreCase(role)) {
            long adminCount = userAccountRepository.findAll().stream()
                    .filter(u -> "ADMIN".equalsIgnoreCase(u.getRole()))
                    .count();
            if (adminCount <= 1) {
                throw new IllegalArgumentException("Cannot change role of last ADMIN user");
            }
        }

        // Check if username changed and if new username exists
        if (!user.getUsername().equals(username)) {
            if (userAccountRepository.findByUsername(username).isPresent()) {
                throw new IllegalArgumentException("Username already exists: " + username);
            }
            user.setUsername(username);
        }

        user.setRole(role.toUpperCase());
        user.setRoomId(roomId);

        UserAccount saved = userAccountRepository.save(user);
        // Reload with room relationship eagerly loaded
        return userAccountRepository.findByIdWithRoom(saved.getId()).orElse(saved);
    }

    /**
     * Change user password
     */
    @Transactional
    public void changePassword(Long id, String newPassword) {
        UserAccount user = userAccountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));

        user.setPassword(passwordEncoder.encode(newPassword));
        userAccountRepository.save(user);
    }

    /**
     * Delete user account
     * @throws IllegalArgumentException if trying to delete last admin
     */
    @Transactional
    public void deleteUser(Long id) {
        UserAccount user = userAccountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));

        // Prevent deleting the last admin
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            long adminCount = userAccountRepository.findAll().stream()
                    .filter(u -> "ADMIN".equalsIgnoreCase(u.getRole()))
                    .count();
            if (adminCount <= 1) {
                throw new IllegalArgumentException("Cannot delete the last ADMIN user");
            }
        }

        userAccountRepository.deleteById(id);
    }

    /**
     * Validate role string
     */
    private boolean isValidRole(String role) {
        if (role == null) return false;
        String upperRole = role.toUpperCase();
        return upperRole.equals("ADMIN") ||
               upperRole.equals("STAFF") ||
               upperRole.equals("USER") ||
               upperRole.equals("GUEST");
    }
}
