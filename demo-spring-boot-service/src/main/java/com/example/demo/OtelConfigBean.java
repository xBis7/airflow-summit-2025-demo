package com.example.demo;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;

import io.opentelemetry.context.propagation.ContextPropagators;

import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;

import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;

import io.opentelemetry.semconv.ServiceAttributes;

import jakarta.annotation.PreDestroy;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OtelConfigBean {

  private SdkTracerProvider tracerProvider;

  @Bean
  public OpenTelemetry openTelemetry() {
    OtlpHttpSpanExporter exporter = OtlpHttpSpanExporter.builder()
                                        .setEndpoint("http://otel-collector:4318/v1/traces")
                                        .build();

    // Create a Resource with the service name.
    Resource serviceNameResource = Resource.getDefault().merge(
        Resource.create(
            Attributes.of(ServiceAttributes.SERVICE_NAME, "Java.Tester")
        )
    );

    // Create a SdkTracerProvider and configure it with a SimpleSpanProcessor that uses an OtlpHttpSpanExporter.
    tracerProvider = SdkTracerProvider.builder()
                         .addSpanProcessor(
                             // Use simple processor to make sure that the span gets exported immediately.
                             SimpleSpanProcessor.builder(exporter).build()
                         )
                         .setResource(serviceNameResource)
                         .build();

    OpenTelemetrySdk sdk = OpenTelemetrySdk.builder()
                               .setTracerProvider(tracerProvider)
                               .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                               .build();

    // Register once for libraries that use GlobalOpenTelemetry
    OpenTelemetrySdk.builder().buildAndRegisterGlobal();

    return sdk;
  }

  @Bean
  public W3CTraceContextPropagator propagator() {
    return W3CTraceContextPropagator.getInstance();
  }

  @PreDestroy
  public void shutdown() {
    if (tracerProvider != null) {
      tracerProvider.close();
    }
  }
}