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

  @GetMapping("/work")
  public String work(HttpServletRequest request) {
    OtelProvider otelProvider = new OtelProvider();
    Tracer tracer = otelProvider.getTracer();

    Context extracted = otelProvider.extractContext(request);
    System.out.println("x: " + extracted);

    Span serverSpan = tracer.spanBuilder("handle /work")
                          .setSpanKind(SpanKind.SERVER)
                          .setParent(extracted)
                          .startSpan();

    try (Scope s = serverSpan.makeCurrent()) {
      serverSpan.setAttribute("http.route", "/work");
      return "ok";
    } finally {
      serverSpan.end();
    }
  }
}
