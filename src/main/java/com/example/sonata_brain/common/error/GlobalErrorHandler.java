package com.example.sonata_brain.common.error;

import com.example.sonata_brain.exception.FeeStructureValidationException;
import com.example.sonata_brain.exception.PaymentValidationException;
import com.example.sonata_brain.exception.StudentNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/** Turns every exception into the standard {@link ApiError} body (REQ-005). Never exposes internals. */
@RestControllerAdvice
public class GlobalErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalErrorHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiError> api(ApiException e) {
        return respond(e.status(), e.getMessage(), e.details());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> invalidBody(MethodArgumentNotValidException e) {
        List<ApiError.FieldProblem> details = e.getBindingResult().getFieldErrors().stream()
                .map(GlobalErrorHandler::problem)
                .toList();
        return respond(HttpStatus.BAD_REQUEST, "Validation failed", details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> unreadable(HttpMessageNotReadableException e) {
        return respond(HttpStatus.BAD_REQUEST, "Malformed request body", null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiError> typeMismatch(MethodArgumentTypeMismatchException e) {
        String message = "Invalid value for '" + e.getName() + "'";
        return respond(HttpStatus.BAD_REQUEST, message, List.of(new ApiError.FieldProblem(e.getName(), message)));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ApiError> missingParam(MissingServletRequestParameterException e) {
        String message = "Parameter '" + e.getParameterName() + "' is required";
        return respond(HttpStatus.BAD_REQUEST, message,
                List.of(new ApiError.FieldProblem(e.getParameterName(), message)));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    ResponseEntity<ApiError> missingHeader(MissingRequestHeaderException e) {
        return respond(HttpStatus.BAD_REQUEST, "Header '" + e.getHeaderName() + "' is required", null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiError> noRoute(NoResourceFoundException e) {
        return respond(HttpStatus.NOT_FOUND, "Not found", null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiError> wrongMethod(HttpRequestMethodNotSupportedException e) {
        return respond(HttpStatus.METHOD_NOT_ALLOWED, "Method " + e.getMethod() + " is not supported here", null);
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ApiError> status(ResponseStatusException e) {
        return respond(e.getStatusCode(), e.getReason() != null ? e.getReason() : "Request failed", null);
    }

    @ExceptionHandler(StudentNotFoundException.class)
    ResponseEntity<ApiError> studentNotFound(StudentNotFoundException e) {
        return respond(HttpStatus.NOT_FOUND, e.getMessage(), null);
    }

    @ExceptionHandler({FeeStructureValidationException.class, PaymentValidationException.class,
            IllegalArgumentException.class})
    ResponseEntity<ApiError> invalid(RuntimeException e) {
        return respond(HttpStatus.BAD_REQUEST, e.getMessage(), null);
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<ApiError> invalidState(IllegalStateException e) {
        return respond(HttpStatus.CONFLICT, e.getMessage(), null);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception e) {
        log.error("Unhandled error", e);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", null);
    }

    private static ApiError.FieldProblem problem(FieldError f) {
        return new ApiError.FieldProblem(f.getField(), f.getDefaultMessage());
    }

    private static ResponseEntity<ApiError> respond(HttpStatusCode code, String message,
                                                    List<ApiError.FieldProblem> details) {
        HttpStatus status = HttpStatus.valueOf(code.value());
        return ResponseEntity.status(status).body(ApiError.of(status, message, details));
    }
}
