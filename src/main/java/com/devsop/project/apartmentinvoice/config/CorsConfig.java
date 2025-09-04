package com.devsop.project.apartmentinvoice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/**")
        // dev: ยอมจาก localhost ทุกพอร์ต (เช่น Vite/Next/React)
        .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")
        .allowedMethods("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .exposedHeaders("Content-Disposition")
        .allowCredentials(false)
        .maxAge(3600);
  }
}
