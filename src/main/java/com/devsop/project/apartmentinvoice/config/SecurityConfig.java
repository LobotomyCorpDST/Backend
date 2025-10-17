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
                .requestMatchers("/error", "/api/auth/**", "/h2-console/**").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // ---------- Guest readable endpoints ----------
                // Allow ROLE_GUEST to view data via GET
                .requestMatchers(HttpMethod.GET, "/api/**").hasAnyAuthority("ROLE_GUEST", "GUEST", "ROLE_ADMIN", "ADMIN", "ROLE_STAFF", "STAFF")

                // ---------- Admin/Staff write permissions ----------
                .requestMatchers(HttpMethod.POST,   "/api/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN", "ROLE_STAFF", "STAFF")
                .requestMatchers(HttpMethod.PUT,    "/api/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN", "ROLE_STAFF", "STAFF")
                .requestMatchers(HttpMethod.PATCH,  "/api/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN", "ROLE_STAFF", "STAFF")
                .requestMatchers(HttpMethod.DELETE, "/api/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN", "ROLE_STAFF", "STAFF")

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
            "http://127.0.0.1:3000"
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
