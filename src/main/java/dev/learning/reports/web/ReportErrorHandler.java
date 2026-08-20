package dev.learning.reports.web;

import dev.learning.reports.storage.InfraiException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ReportErrorHandler {
    @ExceptionHandler(InfraiException.class)
    ResponseEntity<Map<String, String>> infraiError(InfraiException error) {
        HttpStatus status = error.status() >= 400 && error.status() < 500
                ? HttpStatus.valueOf(error.status())
                : HttpStatus.BAD_GATEWAY;
        return ResponseEntity.status(status).body(Map.of(
                "code", error.code(),
                "message", error.getMessage()));
    }
}
