package com.fit.badminton.common;

import java.time.Instant;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
  private ResponseEntity<Map<String, Object>> body(HttpStatusCode  s, String m) {
    return ResponseEntity.status(s).body(Map.of("message", m, "timestamp", Instant.now().toString()));
  }

  @ExceptionHandler(NotFoundException.class)
  ResponseEntity<Map<String, Object>> notFound(NotFoundException e) {
    return body(HttpStatus.NOT_FOUND, e.getMessage());
  }

  @ExceptionHandler(ConflictException.class)
  ResponseEntity<Map<String, Object>> conflict(ConflictException e) {
    return body(HttpStatus.CONFLICT, e.getMessage());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  ResponseEntity<Map<String, Object>> bad(IllegalArgumentException e) {
    return body(HttpStatus.BAD_REQUEST, e.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException e) {
    String m = e.getBindingResult().getFieldErrors().stream().findFirst()
        .map(x -> x.getField() + ": " + x.getDefaultMessage()).orElse("Validation failed");
    return body(HttpStatus.BAD_REQUEST, m);
  }

  @ExceptionHandler(ResponseStatusException.class)
ResponseEntity<Map<String, Object>> responseStatus(ResponseStatusException e) {
    String message = e.getReason() != null
        ? e.getReason()
        : "Request failed";

    return body(e.getStatusCode(), message);
}

   @ExceptionHandler(Exception.class)
  ResponseEntity<Map<String, Object>> unexpected(Exception e) {
    log.error("Unexpected server error", e);
    return body(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error");
  }
}
