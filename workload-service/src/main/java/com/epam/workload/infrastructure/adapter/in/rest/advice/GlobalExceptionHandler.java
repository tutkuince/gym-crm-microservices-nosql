package com.epam.workload.infrastructure.adapter.in.rest.advice;

import com.epam.workload.infrastructure.adapter.in.rest.dto.ApiError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private ApiError build(HttpStatus status, String message, ServletWebRequest req, Map<String,String> fields) {
        return new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                Objects.nonNull(req) && Objects.nonNull(req.getRequest()) ? req.getRequest().getRequestURI() : null,
                (fields == null || fields.isEmpty()) ? null : fields
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, ServletWebRequest req) {
        var fields = new LinkedHashMap<String,String>();
        ex.getBindingResult().getFieldErrors().forEach(fe -> fields.put(fe.getField(), fe.getDefaultMessage()));
        return ResponseEntity.badRequest().body(build(HttpStatus.BAD_REQUEST, "Validation failed", req, fields));
    }

    @ExceptionHandler({ HttpMessageNotReadableException.class, IllegalArgumentException.class })
    public ResponseEntity<ApiError> handleBadRequest(Exception ex, ServletWebRequest req) {
        return ResponseEntity.badRequest().body(build(HttpStatus.BAD_REQUEST, ex.getMessage(), req, null));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiError> handleNotFound(NoSuchElementException ex, ServletWebRequest req) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(build(HttpStatus.NOT_FOUND, ex.getMessage(), req, null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, ServletWebRequest req) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", req, null));
    }
}
