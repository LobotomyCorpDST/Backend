package com.devsop.project.apartmentinvoice.security;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        // ✅ Step 1: Handle Guest token shortcut (from frontend localStorage)
        if (header != null && header.equals("Bearer guest-token")) {
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                List<GrantedAuthority> authorities =
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_GUEST"));

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken("guest", null, authorities);
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }

            filterChain.doFilter(request, response);
            return;
        }

        // ✅ Step 2: Handle regular JWT tokens
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            if (jwtTokenProvider.validate(token)
                    && SecurityContextHolder.getContext().getAuthentication() == null) {

                String username = jwtTokenProvider.getUsername(token);
                String role = jwtTokenProvider.getRole(token); // Already prefixed as ROLE_X

                if (role == null || role.isBlank()) {
                    role = "ROLE_GUEST"; // fallback
                }

                List<GrantedAuthority> authorities =
                        Collections.singletonList(new SimpleGrantedAuthority(role));

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(username, null, authorities);
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);
    }
}
