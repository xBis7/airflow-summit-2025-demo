package com.example.demo.controllers;

import com.example.demo.OtelProvider;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class BaseController {

  private final OtelProvider otelProvider;

  // Code-injection.
  public BaseController(OtelProvider otelProvider) {
    this.otelProvider = otelProvider;
  }

  @GetMapping("/work")
  public String work(HttpServletRequest request) {
    Tracer tracer = otelProvider.getTracer();

    Context extractedCtx = otelProvider.extractContext(request);

    Span span = tracer.spanBuilder("Api work")
            .setSpanKind(SpanKind.SERVER)
            .setParent(extractedCtx)
            .startSpan();

    try (Scope ignored = span.makeCurrent()) {
      span.setAttribute("http.route", "/work");
      // Some work.
      Thread.sleep(10000);
      return "ok";
    } catch (InterruptedException e) {
        throw new RuntimeException(e);
    } finally {
      span.end();
    }
  }
}
