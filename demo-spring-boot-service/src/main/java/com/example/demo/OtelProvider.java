package com.example.demo;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Component
public class OtelProvider {

  private final W3CTraceContextPropagator contextPropagator = W3CTraceContextPropagator.getInstance();
  private final OpenTelemetry openTelemetry;

  public OtelProvider(OpenTelemetry openTelemetry) {
    this.openTelemetry = openTelemetry;
  }

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

  public Tracer getTracer() {
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
