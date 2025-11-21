package com.devsop.project.apartmentinvoice.unit.security;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.devsop.project.apartmentinvoice.security.JwtTokenProvider;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

/**
 * Unit tests for JwtTokenProvider focusing on token generation, validation, and claim extraction.
 */
class JwtTokenProviderUnitTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
    }

    @Test
    void testGenerate_validAdminUser_returnsValidToken() {
        // Arrange
        String username = "admin";
        String role = "ADMIN";

        // Act
        String token = jwtTokenProvider.generate(username, role);

        // Assert
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(jwtTokenProvider.validate(token));

        // Verify claims
        assertEquals(username, jwtTokenProvider.getUsername(token));
        assertEquals("ROLE_ADMIN", jwtTokenProvider.getRole(token));
    }

    @Test
    void testGenerate_validStaffUser_returnsValidToken() {
        // Arrange
        String username = "staff";
        String role = "STAFF";

        // Act
        String token = jwtTokenProvider.generate(username, role);

        // Assert
        assertNotNull(token);
        assertTrue(jwtTokenProvider.validate(token));
        assertEquals(username, jwtTokenProvider.getUsername(token));
        assertEquals("ROLE_STAFF", jwtTokenProvider.getRole(token));
    }

    @Test
    void testGenerate_guestUser_returnsValidToken() {
        // Arrange
        String username = "guest";
        String role = "GUEST";

        // Act
        String token = jwtTokenProvider.generate(username, role);

        // Assert
        assertNotNull(token);
        assertTrue(jwtTokenProvider.validate(token));
        assertEquals(username, jwtTokenProvider.getUsername(token));
        assertEquals("ROLE_GUEST", jwtTokenProvider.getRole(token));
    }

    @Test
    void testGenerate_roleNormalization_addsRolePrefix() {
        // Arrange: Role without "ROLE_" prefix
        String username = "testuser";
        String role = "ADMIN"; // No ROLE_ prefix

        // Act
        String token = jwtTokenProvider.generate(username, role);

        // Assert
        assertEquals("ROLE_ADMIN", jwtTokenProvider.getRole(token)); // Should add ROLE_ prefix
    }

    @Test
    void testGenerate_roleAlreadyNormalized_doesNotDuplicate() {
        // Arrange: Role already has "ROLE_" prefix
        String username = "testuser";
        String role = "ROLE_ADMIN"; // Already has ROLE_ prefix

        // Act
        String token = jwtTokenProvider.generate(username, role);

        // Assert
        assertEquals("ROLE_ADMIN", jwtTokenProvider.getRole(token)); // Should not duplicate prefix
    }

    @Test
    void testGenerate_nullRole_defaultsToGuest() {
        // Arrange
        String username = "testuser";
        String role = null;

        // Act
        String token = jwtTokenProvider.generate(username, role);

        // Assert
        assertEquals("ROLE_GUEST", jwtTokenProvider.getRole(token)); // Should default to ROLE_GUEST
    }

    @Test
    void testGenerate_nullUsername_defaultsToGuest() {
        // Arrange
        String username = null;
        String role = "ADMIN";

        // Act
        String token = jwtTokenProvider.generate(username, role);

        // Assert
        assertEquals("guest", jwtTokenProvider.getUsername(token)); // Should default to "guest"
        assertEquals("ROLE_ADMIN", jwtTokenProvider.getRole(token));
    }

    @Test
    void testGenerate_lowercaseRole_normalizesToUppercase() {
        // Arrange
        String username = "testuser";
        String role = "admin"; // Lowercase

        // Act
        String token = jwtTokenProvider.generate(username, role);

        // Assert
        assertEquals("ROLE_ADMIN", jwtTokenProvider.getRole(token)); // Should normalize to uppercase
    }

    @Test
    void testValidate_validToken_returnsTrue() {
        // Arrange
        String token = jwtTokenProvider.generate("admin", "ADMIN");

        // Act
        boolean isValid = jwtTokenProvider.validate(token);

        // Assert
        assertTrue(isValid);
    }

    @Test
    void testValidate_invalidToken_returnsFalse() {
        // Arrange
        String invalidToken = "invalid.token.here";

        // Act
        boolean isValid = jwtTokenProvider.validate(invalidToken);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void testValidate_malformedToken_returnsFalse() {
        // Arrange
        String malformedToken = "eyJhbGciOiJIUzI1NiJ9.malformed";

        // Act
        boolean isValid = jwtTokenProvider.validate(malformedToken);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void testValidate_expiredToken_returnsFalse() {
        // Arrange: Create an expired token manually
        String secret = "change-this-secret-to-32+chars-change-this-secret";
        Key key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        String expiredToken = Jwts.builder()
                .setSubject("testuser")
                .claim("role", "ROLE_ADMIN")
                .setIssuedAt(new Date(System.currentTimeMillis() - 1000 * 60 * 60 * 24)) // 24 hours ago
                .setExpiration(new Date(System.currentTimeMillis() - 1000 * 60 * 60)) // Expired 1 hour ago
                .signWith(key)
                .compact();

        // Act
        boolean isValid = jwtTokenProvider.validate(expiredToken);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void testValidate_tokenWithWrongSignature_returnsFalse() {
        // Arrange: Create token with different secret
        String differentSecret = "different-secret-key-for-testing-purposes";
        Key wrongKey = Keys.hmacShaKeyFor(differentSecret.getBytes(StandardCharsets.UTF_8));

        String tokenWithWrongSignature = Jwts.builder()
                .setSubject("testuser")
                .claim("role", "ROLE_ADMIN")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
                .signWith(wrongKey)
                .compact();

        // Act
        boolean isValid = jwtTokenProvider.validate(tokenWithWrongSignature);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void testGetUsername_validToken_returnsCorrectUsername() {
        // Arrange
        String expectedUsername = "john.doe";
        String token = jwtTokenProvider.generate(expectedUsername, "ADMIN");

        // Act
        String actualUsername = jwtTokenProvider.getUsername(token);

        // Assert
        assertEquals(expectedUsername, actualUsername);
    }

    @Test
    void testGetRole_validToken_returnsCorrectRole() {
        // Arrange
        String token = jwtTokenProvider.generate("testuser", "STAFF");

        // Act
        String role = jwtTokenProvider.getRole(token);

        // Assert
        assertEquals("ROLE_STAFF", role);
    }

    @Test
    void testGetRole_tokenWithoutRoleClaim_defaultsToGuest() {
        // Arrange: Create token without role claim
        String secret = "change-this-secret-to-32+chars-change-this-secret";
        Key key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        String tokenWithoutRole = Jwts.builder()
                .setSubject("testuser")
                // No role claim
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
                .signWith(key)
                .compact();

        // Act
        String role = jwtTokenProvider.getRole(tokenWithoutRole);

        // Assert
        assertEquals("ROLE_GUEST", role); // Should default to ROLE_GUEST
    }

    @Test
    void testTokenGeneration_hasCorrectStructure() {
        // Arrange
        String token = jwtTokenProvider.generate("admin", "ADMIN");

        // Act & Assert
        // JWT tokens have 3 parts separated by dots
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length, "JWT token should have 3 parts (header.payload.signature)");
    }

    @Test
    void testMultipleTokensGeneration_produceDifferentTokens() {
        // Arrange & Act
        String token1 = jwtTokenProvider.generate("user1", "ADMIN");

        // Wait a bit to ensure different timestamp (JWT uses seconds precision)
        try {
            Thread.sleep(1100); // Wait slightly over 1 second
        } catch (InterruptedException e) {
            // Ignore
        }

        String token2 = jwtTokenProvider.generate("user1", "ADMIN");

        // Assert
        // Even with same user/role, tokens should be different due to different issuedAt timestamps
        assertNotEquals(token1, token2);
    }

    @Test
    void testTokenGeneration_withSpecialCharactersInUsername() {
        // Arrange
        String username = "user@example.com";
        String role = "ADMIN";

        // Act
        String token = jwtTokenProvider.generate(username, role);

        // Assert
        assertTrue(jwtTokenProvider.validate(token));
        assertEquals(username, jwtTokenProvider.getUsername(token));
    }

    @Test
    void testTokenGeneration_withThaiCharactersInUsername() {
        // Arrange
        String username = "ผู้ใช้งาน";
        String role = "ADMIN";

        // Act
        String token = jwtTokenProvider.generate(username, role);

        // Assert
        assertTrue(jwtTokenProvider.validate(token));
        assertEquals(username, jwtTokenProvider.getUsername(token));
    }
}
