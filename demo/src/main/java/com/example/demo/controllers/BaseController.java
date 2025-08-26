package com.example.demo.controllers;

import io.opentelemetry.api.trace.Span;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class BaseController {

  @GetMapping("/ping")
  public Map<String, Object> ping() {
    // Java agent will create a SERVER span and extract incoming context automatically.
    doSubtask();
    Span s = Span.current();
    return Map.of(
        "status", "ok",
        "traceId", s.getSpanContext().getTraceId(),
        "spanId", s.getSpanContext().getSpanId()
    );
  }

  void doSubtask() {
    try {
      Thread.sleep(30);
    } catch (InterruptedException ignored) {}
  }
}
