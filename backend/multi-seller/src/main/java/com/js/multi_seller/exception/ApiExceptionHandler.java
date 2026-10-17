package com.js.multi_seller.exception;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(BadRequestException.class)
    ResponseEntity<ApiError> handleBadRequest(BadRequestException exception) {
        return response(HttpStatus.BAD_REQUEST, "BAD_REQUEST", exception.getMessage(), List.of());
    }

    @ExceptionHandler(UnauthorizedException.class)
    ResponseEntity<ApiError> handleUnauthorized(UnauthorizedException exception) {
        return response(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", exception.getMessage(), List.of());
    }

    @ExceptionHandler(ForbiddenException.class)
    ResponseEntity<ApiError> handleForbidden(ForbiddenException exception) {
        return response(HttpStatus.FORBIDDEN, "FORBIDDEN", exception.getMessage(), List.of());
    }

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ApiError> handleNotFound(NotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, "NOT_FOUND", exception.getMessage(), List.of());
    }

    @ExceptionHandler({ConflictException.class, OptimisticLockingFailureException.class})
    ResponseEntity<ApiError> handleConflict(RuntimeException exception) {
        String message = exception instanceof ConflictException
                ? exception.getMessage()
                : "The listing was modified by another request; refresh and try again";
        return response(HttpStatus.CONFLICT, "CONFLICT", message, List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        String field = exception.getName();
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Invalid value for " + field,
                List.of(new FieldError(field, "Invalid value")));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        List<FieldError> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldError(error.getField(), error.getDefaultMessage()))
                .toList();
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed", fieldErrors);
    }

    private ResponseEntity<ApiError> response(HttpStatus status, String code, String message, List<FieldError> fieldErrors) {
        return ResponseEntity.status(status).body(new ApiError(Instant.now(), status.value(), code, message, fieldErrors));
    }
}
