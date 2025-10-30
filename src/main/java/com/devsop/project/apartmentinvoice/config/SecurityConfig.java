package com.devsop.project.apartmentinvoice.config;

import java.time.Duration;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.devsop.project.apartmentinvoice.security.JwtAuthFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .headers(h -> h.frameOptions(f -> f.disable()))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(auth -> auth
                // ---------- Public endpoints ----------
                .requestMatchers("/", "/error", "/health", "/api/auth/**", "/h2-console/**", "/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // ---------- ADMIN-only endpoints (User Management) ----------
                .requestMatchers("/api/users/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN")

                // ---------- STAFF/USER-specific endpoints (Dashboard + Maintenance only) ----------
                .requestMatchers(HttpMethod.GET, "/api/dashboard/**", "/api/maintenance/**", "/api/rooms").hasAnyAuthority("ROLE_ADMIN", "ADMIN", "ROLE_STAFF", "STAFF", "ROLE_USER", "USER")
                .requestMatchers(HttpMethod.POST, "/api/maintenance/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN", "ROLE_STAFF", "STAFF", "ROLE_USER", "USER")
                .requestMatchers(HttpMethod.PUT, "/api/maintenance/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN", "ROLE_STAFF", "STAFF")
                .requestMatchers(HttpMethod.PATCH, "/api/maintenance/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN", "ROLE_STAFF", "STAFF")
                .requestMatchers(HttpMethod.DELETE, "/api/maintenance/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN", "ROLE_STAFF", "STAFF")

                // ---------- USER role (Read + Create Maintenance) ----------
                .requestMatchers(HttpMethod.GET, "/api/**").hasAnyAuthority("ROLE_GUEST", "GUEST", "ROLE_USER", "USER", "ROLE_ADMIN", "ADMIN", "ROLE_STAFF", "STAFF")

                // ---------- ADMIN full write permissions ----------
                .requestMatchers(HttpMethod.POST, "/api/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN")

                // ---------- Everything else ----------
                .anyRequest().authenticated()
            )

            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of(
            "http://localhost:3000",
            "http://127.0.0.1:3000",
            "http://localhost:32080",
            "http://127.0.0.1:32080",
            "http://localhost:32033",
            "http://127.0.0.1:32033"
        ));
        cfg.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization","Content-Type","Accept","Origin"));
        cfg.setExposedHeaders(List.of("Authorization"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(Duration.ofHours(1));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
