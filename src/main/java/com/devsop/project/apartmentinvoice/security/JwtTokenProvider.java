package com.devsop.project.apartmentinvoice.security;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtTokenProvider {

    private static final String SECRET = "change-this-secret-to-32+chars-change-this-secret";
    private static final long EXPIRE_MS = 1000L * 60 * 60 * 12; // 12 hours
    private static final String CLAIM_ROLE = "role";

    private Key key() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    // ✅ Generate token for any user or guest
    public String generate(String username, String role) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + EXPIRE_MS);

        // Normalize role (so it always has ROLE_ prefix)
        String normalizedRole = (role == null) ? "ROLE_GUEST" :
                (role.startsWith("ROLE_") ? role : "ROLE_" + role.toUpperCase());

        return Jwts.builder()
                .setSubject(username == null ? "guest" : username)
                .claim(CLAIM_ROLE, normalizedRole)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validate(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key()).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String getUsername(String token) {
        return Jwts.parserBuilder().setSigningKey(key()).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    public String getRole(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(key()).build()
                .parseClaimsJws(token).getBody();
        Object v = claims.get(CLAIM_ROLE);
        return v == null ? "ROLE_GUEST" : v.toString(); // default to ROLE_GUEST if missing
    }
}
