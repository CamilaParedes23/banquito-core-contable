package com.banquito.core.accounting.shared.exception;

import com.banquito.core.accounting.api.dto.api.ErrorResponse;
import com.banquito.core.accounting.shared.tracing.CorrelationIdHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
        return ResponseEntity.status(ex.getStatus()).body(new ErrorResponse(LocalDateTime.now(), CorrelationIdHolder.get(), ex.getCode(), ex.getMessage(), List.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream().map(e -> e.getField() + ": " + e.getDefaultMessage()).toList();
        return ResponseEntity.badRequest().body(new ErrorResponse(LocalDateTime.now(), CorrelationIdHolder.get(), "VALIDATION_ERROR", "Error de validación", details));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(
                LocalDateTime.now(),
                CorrelationIdHolder.get(),
                "SECURITY_ACCESS_DENIED",
                "Acceso denegado. El token no posee permisos para este recurso.",
                List.of()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(LocalDateTime.now(), CorrelationIdHolder.get(), "INTERNAL_ERROR", "Error interno no controlado", List.of(ex.getClass().getSimpleName())));
    }
}
