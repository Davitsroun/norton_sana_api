package com.leang.authservice.exception;


import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.time.Instant;
import java.util.Map;

public class ConflictException extends RuntimeException {
  private final Map<String, String> fields;

  public ConflictException(String message, Map<String, String> fields) {
    super(message);
    this.fields = fields;
  }

  public ProblemDetail toProblemDetail() {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, getMessage());
    pd.setTitle("Duplicate Field");
    pd.setProperty("timestamp", Instant.now());
    if (fields != null && !fields.isEmpty()) {
      pd.setProperty("fields", fields); // e.g. { "username": "...", "email": "..." }
    }
    return pd;
  }
}

