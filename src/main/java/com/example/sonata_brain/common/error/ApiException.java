package com.example.sonata_brain.common.error;

import org.springframework.http.HttpStatus;

import java.util.List;

/** A business error with the HTTP status it maps to. Thrown from services; rendered by {@link GlobalErrorHandler}. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final List<ApiError.FieldProblem> details;

    public ApiException(HttpStatus status, String message, List<ApiError.FieldProblem> details) {
        super(message);
        this.status = status;
        this.details = details == null ? List.of() : details;
    }

    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, message, null);
    }

    public static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, message, null);
    }

    public static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message, null);
    }

    /** 400 pointing at one request field. */
    public static ApiException invalidField(String field, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message, List.of(new ApiError.FieldProblem(field, message)));
    }

    public static ApiException unauthorized(String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, message, null);
    }

    public HttpStatus status() {
        return status;
    }

    public List<ApiError.FieldProblem> details() {
        return details;
    }
}
