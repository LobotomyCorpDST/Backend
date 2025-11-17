package com.devsop.project.apartmentinvoice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporterBuilder;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;

@Configuration
public class OpenTelemetryConfig {

  @Bean(destroyMethod = "shutdown")
  public OtlpGrpcSpanExporter otlpGrpcSpanExporter(
    @Value("${observability.otel.endpoint:http://otel-collector-svc.doomed-apt.svc.cluster.local:4318/v1/traces}") String endpoint,
    @Value("${observability.otel.headers:}") String headers
  ) {
    OtlpGrpcSpanExporterBuilder builder = OtlpGrpcSpanExporter.builder()
      .setEndpoint(endpoint);

    if (headers != null && !headers.isBlank()) {
      for (String headerPair : headers.split(",")) {
        String[] kv = headerPair.split("=");
        if (kv.length == 2) {
          builder.addHeader(kv[0].trim(), kv[1].trim());
        }
      }
    }

    return builder.build();
  }

  @Bean(destroyMethod = "close")
  public SdkTracerProvider sdkTracerProvider(
      Environment environment,
      OtlpGrpcSpanExporter spanExporter,
      @Value("${management.tracing.sampling.probability:1.0}") double samplingProbability
  ) {
    String appName = environment.getProperty("spring.application.name", "apartment-invoice");
    String profile = environment.getProperty("spring.profiles.active", "default");

    Resource resource = Resource.getDefault().merge(Resource.create(
      Attributes.builder()
        .put("service.name", appName)
        .put("deployment.environment", profile)
        .build()
    ));

    return SdkTracerProvider.builder()
      .setResource(resource)
      .addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build())
      .setSampler(Sampler.parentBased(Sampler.traceIdRatioBased(samplingProbability)))
      .build();
  }

  @Bean(destroyMethod = "close")
  public OpenTelemetrySdk openTelemetrySdk(SdkTracerProvider sdkTracerProvider) {
    return OpenTelemetrySdk.builder()
      .setTracerProvider(sdkTracerProvider)
      .setPropagators(ContextPropagators.create(
        TextMapPropagator.composite(
          W3CTraceContextPropagator.getInstance(),
          W3CBaggagePropagator.getInstance()
        )
      ))
      .build();
  }

  @Bean
  public OpenTelemetry openTelemetry(OpenTelemetrySdk sdk) {
    return sdk;
  }
}
