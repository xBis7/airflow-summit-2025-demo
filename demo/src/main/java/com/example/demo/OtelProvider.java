package com.example.demo;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;

import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporterBuilder;

import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;

import io.opentelemetry.semconv.ServiceAttributes;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;

public class OtelProvider {

  public static final TextMapSetter<HttpHeaders> SETTER = (carrier, key, value) -> {
    if (carrier != null) {
      carrier.add(key, value);
    }
  };

  public static final TextMapGetter<HttpServletRequest> GETTER = new TextMapGetter<>() {
    @Override public Iterable<String> keys(HttpServletRequest carrier) {
      return carrier.getHeaderNames()::asIterator;
    }
    @Override public String get(HttpServletRequest carrier, String key) {
      return carrier.getHeader(key);
    }
  };

  private final W3CTraceContextPropagator contextPropagator = W3CTraceContextPropagator.getInstance();

  public Tracer getTracer() {
    SpanExporter exporter;

    OtlpHttpSpanExporterBuilder httpSpanExporterBuilder = OtlpHttpSpanExporter.builder();
    exporter = httpSpanExporterBuilder
                   .setEndpoint("http://otel-collector:4318/v1/traces")
                   .build();

    // Create a Resource with the service name.
    Resource serviceNameResource = Resource.getDefault().merge(
        Resource.create(
            Attributes.of(ServiceAttributes.SERVICE_NAME, "Java.Tester")
        )
    );

    // Create a SdkTracerProvider and configure it with a SimpleSpanProcessor that uses an OtlpHttpSpanExporter.
    SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                                           .addSpanProcessor(
                                              // Use simple processor to make sure that the span gets exported immediately.
                                              SimpleSpanProcessor.builder(exporter).build()
                                           )
                                           .setResource(serviceNameResource)
                                           .build();

    // Build an OpenTelemetry instance from the configured SdkTracerProvider.
    OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
                                      .setTracerProvider(tracerProvider)
                                      .buildAndRegisterGlobal();

    // Get and return the tracer.
    return openTelemetry.getTracer("com.example.opentelemetry", "1.0.0");
  }

  public Context extractContext(HttpServletRequest request) {
    return contextPropagator.extract(Context.current(), request, GETTER);
  }

  public HttpHeaders injectContext() {
    HttpHeaders headers = new HttpHeaders();
    contextPropagator.inject(Context.current(), headers, SETTER);
    return headers;
  }
}
