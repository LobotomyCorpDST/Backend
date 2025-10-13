package com.devsop.project.apartmentinvoice.security;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtTokenProvider {

    private static final String SECRET = "change-this-secret-to-32+chars-change-this-secret";
    private static final long   EXPIRE_MS = 1000L * 60 * 60 * 12;
    private static final String CLAIM_ROLE = "role";

    private Key key() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    public String generate(String username, String role) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + EXPIRE_MS);

        return Jwts.builder()
                .setSubject(username)
                .claim(CLAIM_ROLE, role)
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
        Object v = Jwts.parserBuilder().setSigningKey(key()).build()
                .parseClaimsJws(token).getBody().get(CLAIM_ROLE);
        return v == null ? null : v.toString();
    }
}
