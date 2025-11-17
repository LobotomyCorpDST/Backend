package com.devsop.project.apartmentinvoice.config;

import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import io.micrometer.core.instrument.MeterRegistry;

@Configuration
public class ObservabilityConfig {

  @Bean
  MeterRegistryCustomizer<MeterRegistry> meterRegistryCustomizer(Environment environment) {
    return registry -> {
      String appName = environment.getProperty("spring.application.name", "apartment-invoice");
      String[] activeProfiles = environment.getActiveProfiles();
      String profile = activeProfiles != null && activeProfiles.length > 0
        ? String.join(",", activeProfiles)
        : environment.getProperty("spring.profiles.active", "default");

      registry.config().commonTags("application", appName, "profile", profile);
    };
  }
}
