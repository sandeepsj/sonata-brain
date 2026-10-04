package com.example.sonata_brain.common.error;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;

/** The one error body every 4xx/5xx response uses (REQ-005.AC1). */
public record ApiError(int status, String error, String message, List<FieldProblem> details, String timestamp) {

    public record FieldProblem(String field, String message) {
    }

    public static ApiError of(HttpStatus status, String message, List<FieldProblem> details) {
        return new ApiError(status.value(), status.getReasonPhrase(), message,
                details == null ? List.of() : details, LocalDateTime.now().toString());
    }
}
