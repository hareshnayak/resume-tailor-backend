package com.resumetailor.controller;

import com.resumetailor.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.exception.TikaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Instant;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(
                ErrorResponse.builder()
                        .error("BAD_REQUEST")
                        .message(ex.getMessage())
                        .timestamp(Instant.now())
                        .build()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");

        log.warn("Validation error: {}", message);
        return ResponseEntity.badRequest().body(
                ErrorResponse.builder()
                        .error("VALIDATION_ERROR")
                        .message(message)
                        .timestamp(Instant.now())
                        .build()
        );
    }

    @ExceptionHandler(TikaException.class)
    public ResponseEntity<ErrorResponse> handleTikaException(TikaException ex) {
        log.error("CV parsing error: {}", ex.getMessage());
        return ResponseEntity.unprocessableEntity().body(
                ErrorResponse.builder()
                        .error("CV_PARSE_ERROR")
                        .message("Failed to parse the uploaded CV file. Please ensure it is a valid document format (PDF, DOCX, etc.).")
                        .timestamp(Instant.now())
                        .build()
        );
    }

    @ExceptionHandler(SocketTimeoutException.class)
    public ResponseEntity<ErrorResponse> handleTimeout(SocketTimeoutException ex) {
        log.error("Timeout error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(
                ErrorResponse.builder()
                        .error("TIMEOUT")
                        .message("The request timed out. Please try again later.")
                        .timestamp(Instant.now())
                        .build()
        );
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ErrorResponse> handleIOException(IOException ex) {
        log.error("IO error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
                ErrorResponse.builder()
                        .error("SCRAPING_ERROR")
                        .message("Failed to access the provided URL: " + ex.getMessage())
                        .timestamp(Instant.now())
                        .build()
        );
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        log.warn("File too large: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(
                ErrorResponse.builder()
                        .error("FILE_TOO_LARGE")
                        .message("The uploaded file exceeds the maximum allowed size of 10MB.")
                        .timestamp(Instant.now())
                        .build()
        );
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntime(RuntimeException ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);

        // Check for AI timeout indicators
        if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("timeout")) {
            return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(
                    ErrorResponse.builder()
                            .error("AI_TIMEOUT")
                            .message("The AI model timed out while processing your request. This may happen with large documents. Please try again.")
                            .timestamp(Instant.now())
                            .build()
            );
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ErrorResponse.builder()
                        .error("INTERNAL_ERROR")
                        .message(ex.getMessage())
                        .timestamp(Instant.now())
                        .build()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ErrorResponse.builder()
                        .error("INTERNAL_ERROR")
                        .message("An unexpected error occurred. Please try again later.")
                        .timestamp(Instant.now())
                        .build()
        );
    }
}
