package com.devsop.project.apartmentinvoice.config;

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
                // ---------- 1. PUBLIC ENDPOINTS (must stay first) ----------
                .requestMatchers("/", "/error", "/health", "/api/auth/**", "/h2-console/**", "/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api", "/api/").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // ---------- 2. SPECIFIC ROLE ENDPOINTS ----------
                .requestMatchers("/api/users/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/dashboard/**", "/api/maintenance/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN", "ROLE_STAFF", "STAFF", "ROLE_USER", "USER")
                .requestMatchers(HttpMethod.POST, "/api/maintenance/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN", "ROLE_STAFF", "STAFF", "ROLE_USER", "USER")
                .requestMatchers(HttpMethod.PUT, "/api/maintenance/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN", "ROLE_STAFF", "STAFF")
                .requestMatchers(HttpMethod.PATCH, "/api/maintenance/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN", "ROLE_STAFF", "STAFF")
                .requestMatchers(HttpMethod.DELETE, "/api/maintenance/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN", "ROLE_STAFF", "STAFF")
                .requestMatchers(HttpMethod.GET, "/api/**").hasAnyAuthority("ROLE_GUEST", "GUEST", "ROLE_USER", "USER", "ROLE_ADMIN", "ADMIN", "ROLE_STAFF", "STAFF")

                // ---------- 3. GENERAL ADMIN RULES ----------
                .requestMatchers(HttpMethod.POST, "/api/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN")

                // ---------- 4. EVERYTHING ELSE ----------
                .anyRequest().authenticated()
            )

            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOriginPatterns(List.of(
            "http://localhost:*",
            "http://127.0.0.1:*",
            "https://apt.krentiz.dev",
            "https://*.apt.krentiz.dev"
        ));
        cfg.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization","Content-Type","Accept","Origin"));
        cfg.setExposedHeaders(List.of("Authorization"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
