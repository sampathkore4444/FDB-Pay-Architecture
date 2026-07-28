package com.fdbpay.common.exceptions;

import com.fdbpay.common.constants.ErrorCodes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(BusinessException ex) {
        log.warn("Business exception: {} - {}", ex.getCode(), ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            errors.put(field, message);
        });
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("error", Map.of(
                "code", ErrorCodes.VALIDATION_ERROR,
                "message", "Validation failed",
                "details", errors
        ));
        body.put("timestamp", OffsetDateTime.now().toString());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Unexpected exception", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCodes.INTERNAL_ERROR, "An unexpected error occurred");
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String code, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("error", Map.of("code", code, "message", message));
        body.put("timestamp", OffsetDateTime.now().toString());
        return ResponseEntity.status(status).body(body);
    }
}
