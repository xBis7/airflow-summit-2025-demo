package com.example.demo;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporterBuilder;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.semconv.ServiceAttributes;
import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.Map;

public class OtelProvider {

  public static final TextMapSetter<Map<String, String>> SETTER = Map::put;
//  public static final TextMapGetter<Map<String, String>> GETTER =
//      new TextMapGetter<Map<String, String>>() {
//        @Override
//        public Iterable<String> keys(Map<String, String> carrier) {
//          return carrier.keySet();
//        }
//
//        @Override
//        public String get(Map<String, String> carrier, String key) {
//          return carrier.get(key);
//        }
//      };

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

    OtlpGrpcSpanExporterBuilder grpcSpanExporterBuilder = OtlpGrpcSpanExporter.builder();
    exporter = grpcSpanExporterBuilder
                   .setEndpoint("http://otel-collector:4318")
                   .build();

    // 1. Create a Resource with the service name
    Resource serviceNameResource = Resource.getDefault().merge(
        Resource.create(
            Attributes.of(ServiceAttributes.SERVICE_NAME, "Java.Tester")
        )
    );

    // 1. Create a SdkTracerProvider and configure it with a BatchSpanProcessor that uses an OtlpSpanExporter
    SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                                           .addSpanProcessor(
//                                               BatchSpanProcessor.builder(exporter)
//                                                   // This will make sure that the span is exported fast.
//                                                   .setScheduleDelay(java.time.Duration.ofMillis(500))
//                                                   .build()
                                               SimpleSpanProcessor.builder(exporter).build()
                                           )
                                           .setResource(serviceNameResource)
                                           .build();

    // 2. Build an OpenTelemetry instance from the configured SdkTracerProvider
    OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
                                      .setTracerProvider(tracerProvider)
                                      .buildAndRegisterGlobal();

    // 3. Get the tracer
    return openTelemetry.getTracer("com.example.opentelemetry", "1.0.0");
  }

  public Context extractContext(HttpServletRequest request) {
    return contextPropagator.extract(Context.current(), request, GETTER);
  }

//  public void createTestSpans(Tracer tracer) {
//    // Root span.
//    Span rootSpan = tracer.spanBuilder("root-span").startSpan();
//
//    Map<String, String> rootCarrier = new HashMap<>();
//
//    try (Scope ignored = rootSpan.makeCurrent()) {
//      // Since the root Span was made the current span, we can access its context.
//      contextPropagator.inject(Context.current(), rootCarrier, SETTER);
//
//      Context rootContext = contextPropagator.extract(Context.current(), rootCarrier, GETTER);
//
//      for (int i = 0; i < 3; i++) {
//        Span childSpan =
//            tracer.spanBuilder("sub-span-" + i)
//                .setParent(rootContext)
//                .startSpan();
//
//        childSpan.makeCurrent();
//
//        System.out.println("Iteration '" + i + "'.");
//        childSpan.end();
//
//        try {
//          Thread.sleep(10000);
//        } catch (InterruptedException e) {
//          throw new RuntimeException(e);
//        }
//      }
//    } finally {
//      rootSpan.end();
//    }
//  }
}
