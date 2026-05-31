package com.ragenx.pv.common.error;

import com.ragenx.pv.common.response.ApiResponse;
import com.ragenx.pv.common.response.ResponseFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Single place that turns exceptions into the standard error envelope. Anything that is
 * not a known, typed failure becomes {@code system.unexpected}: the stacktrace is logged
 * but never returned to the caller. See docs/conventions.md §7.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Object>> handleApiException(ApiException ex) {
        ErrorCode code = ex.getErrorCode();
        log.warn("api.error code={} message={}", code.getCode(), ex.getMessage());
        return build(code, ex.getMessage(), ex.getDetails());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fields.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        log.warn("api.validation_failed fields={}", fields.keySet());
        return build(ErrorCode.VALIDATION_MISSING_FIELD, "Request validation failed.", fields);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleUnreadableBody(HttpMessageNotReadableException ex) {
        log.warn("api.bad_body message={}", ex.getMostSpecificCause().getMessage());
        return build(ErrorCode.VALIDATION_BAD_FORMAT, "Malformed or unreadable request body.", null);
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ApiResponse<Object>> handleNotFound(Exception ex) {
        log.warn("api.not_found message={}", ex.getMessage());
        return build(ErrorCode.RESOURCE_NOT_FOUND, "The requested resource was not found.", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleUnexpected(Exception ex) {
        // Log the full stacktrace; never leak internals to the caller.
        log.error("api.unexpected", ex);
        return build(ErrorCode.SYSTEM_UNEXPECTED, "An unexpected error occurred.", null);
    }

    private ResponseEntity<ApiResponse<Object>> build(ErrorCode code, String message, Object details) {
        return ResponseEntity.status(code.getHttpStatus())
                .body(ResponseFactory.error(code, message, details));
    }
}
