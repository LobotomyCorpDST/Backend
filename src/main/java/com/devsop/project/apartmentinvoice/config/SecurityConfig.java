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
            // REST API -> ปกติปิด CSRF (หรือจัดการ token เอง)
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .headers(h -> h.frameOptions(f -> f.disable())) // ใช้กับ H2-console
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(auth -> auth
                // ---------- Public endpoints ----------
                .requestMatchers("/error", "/api/auth/**", "/h2-console/**").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // View routes (HTML/PDF ที่ไม่ใช่ /api)
                .requestMatchers(HttpMethod.GET, "/invoices/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/leases/**").permitAll()

                // ให้เปิดดู/สั่งพิมพ์ผ่าน API
                .requestMatchers(HttpMethod.GET, "/api/invoices/*/pdf").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/invoices/*/print").permitAll() // 👈 รองรับลิงก์เดิมที่ปุ่มยังเรียก

                // ช่วงพัฒนา: อ่านข้อมูล invoice ทั้งหมด
                .requestMatchers(HttpMethod.GET, "/api/invoices/**").permitAll()

                // การแก้ไขข้อมูลใบแจ้งหนี้ -> ต้องล็อกอิน
                .requestMatchers(HttpMethod.POST,   "/api/invoices").authenticated()
                .requestMatchers(HttpMethod.PATCH,  "/api/invoices/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/invoices/**").authenticated()

                // ตัวอย่างเดิมสำหรับ rooms (คงไว้)
                .requestMatchers(HttpMethod.POST,   "/api/rooms/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/rooms/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN")
                .requestMatchers(HttpMethod.PATCH,  "/api/rooms/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/rooms/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN")

                // อื่น ๆ ต้องล็อกอิน
                .anyRequest().authenticated()
            )

            // ใส่ JWT filter ไว้ก่อน UsernamePasswordAuthenticationFilter
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
