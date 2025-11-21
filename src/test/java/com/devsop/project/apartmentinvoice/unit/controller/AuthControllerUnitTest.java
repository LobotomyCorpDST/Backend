package com.devsop.project.apartmentinvoice.unit.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.devsop.project.apartmentinvoice.controller.AuthController;
import com.devsop.project.apartmentinvoice.controller.AuthController.LoginRequest;
import com.devsop.project.apartmentinvoice.controller.AuthController.LoginResponse;
import com.devsop.project.apartmentinvoice.controller.AuthController.SimpleUser;
import com.devsop.project.apartmentinvoice.entity.UserAccount;
import com.devsop.project.apartmentinvoice.repository.UserAccountRepository;
import com.devsop.project.apartmentinvoice.security.JwtTokenProvider;

/**
 * Unit tests for AuthController focusing on login and registration flows.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerUnitTest {

    @Mock
    private UserAccountRepository userRepo;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        // Setup is handled by @ExtendWith(MockitoExtension.class)
    }

    // ==================== LOGIN TESTS ====================

    @Test
    void testLogin_guestCredentials_returnsGuestToken() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUsername("guest");
        request.setPassword("guest123");

        String expectedToken = "mock-guest-token";
        when(jwtTokenProvider.generate("guest", "GUEST")).thenReturn(expectedToken);

        // Act
        ResponseEntity<?> response = authController.login(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof LoginResponse);

        LoginResponse loginResponse = (LoginResponse) response.getBody();
        assertEquals(expectedToken, loginResponse.getToken());
        assertEquals("guest", loginResponse.getUsername());
        assertEquals("GUEST", loginResponse.getRole());

        verify(jwtTokenProvider).generate("guest", "GUEST");
        // Guest login should NOT query database
        verify(userRepo, never()).findByUsername(any());
    }

    @Test
    void testLogin_validAdminUser_returnsToken() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("1234");

        UserAccount adminUser = new UserAccount();
        adminUser.setId(1L);
        adminUser.setUsername("admin");
        adminUser.setPassword("$2a$10$encodedPassword");
        adminUser.setRole("ADMIN");

        String expectedToken = "mock-admin-token";

        when(userRepo.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches("1234", "$2a$10$encodedPassword")).thenReturn(true);
        when(jwtTokenProvider.generate("admin", "ADMIN")).thenReturn(expectedToken);

        // Act
        ResponseEntity<?> response = authController.login(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        LoginResponse loginResponse = (LoginResponse) response.getBody();
        assertEquals(expectedToken, loginResponse.getToken());
        assertEquals("admin", loginResponse.getUsername());
        assertEquals("ADMIN", loginResponse.getRole());

        verify(userRepo).findByUsername("admin");
        verify(passwordEncoder).matches("1234", "$2a$10$encodedPassword");
        verify(jwtTokenProvider).generate("admin", "ADMIN");
    }

    @Test
    void testLogin_invalidUsername_returns401() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUsername("nonexistent");
        request.setPassword("password");

        when(userRepo.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // Act
        ResponseEntity<?> response = authController.login(request);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());

        verify(userRepo).findByUsername("nonexistent");
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void testLogin_invalidPassword_returns401() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("wrongPassword");

        UserAccount adminUser = new UserAccount();
        adminUser.setUsername("admin");
        adminUser.setPassword("$2a$10$encodedPassword");
        adminUser.setRole("ADMIN");

        when(userRepo.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches("wrongPassword", "$2a$10$encodedPassword")).thenReturn(false);

        // Act
        ResponseEntity<?> response = authController.login(request);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

        verify(userRepo).findByUsername("admin");
        verify(passwordEncoder).matches("wrongPassword", "$2a$10$encodedPassword");
        verify(jwtTokenProvider, never()).generate(any(), any());
    }

    // ==================== REGISTER TESTS ====================

    @Test
    void testRegister_newUser_createsStaffUser() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUsername("newuser");
        request.setPassword("password123");

        when(userRepo.findByUsername("newuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encodedPassword");

        UserAccount savedUser = new UserAccount();
        savedUser.setId(10L);
        savedUser.setUsername("newuser");
        savedUser.setPassword("$2a$10$encodedPassword");
        savedUser.setRole("STAFF");

        when(userRepo.save(any(UserAccount.class))).thenReturn(savedUser);

        // Act
        ResponseEntity<?> response = authController.register(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof SimpleUser);

        SimpleUser simpleUser = (SimpleUser) response.getBody();
        assertEquals("newuser", simpleUser.getUsername());
        assertEquals("STAFF", simpleUser.getRole());

        verify(userRepo).findByUsername("newuser");
        verify(passwordEncoder).encode("password123");
        verify(userRepo).save(argThat(user ->
            "newuser".equals(user.getUsername()) &&
            "STAFF".equals(user.getRole()) &&
            "$2a$10$encodedPassword".equals(user.getPassword())
        ));
    }

    @Test
    void testRegister_duplicateUsername_returnsBadRequest() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("password");

        UserAccount existingUser = new UserAccount();
        existingUser.setUsername("admin");

        when(userRepo.findByUsername("admin")).thenReturn(Optional.of(existingUser));

        // Act
        ResponseEntity<?> response = authController.register(request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        verify(userRepo).findByUsername("admin");
        verify(userRepo, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void testLogout_returnsSuccessMessage() {
        // Act
        ResponseEntity<?> response = authController.logout();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }
}
