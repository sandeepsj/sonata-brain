package com.example.sonata_brain.common.logging;

import com.example.sonata_brain.common.context.RequestContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Runs first on every request (before Spring Security, so 401/403 are seen too). Sets the request context, returns
 * {@code X-Request-Id}, and writes one REQUEST log entry with status, duration and masked bodies (REQ-008).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String SESSION_ID_HEADER = "X-Session-Id";

    private static final Pattern VALID_REQUEST_ID = Pattern.compile("[A-Za-z0-9-]{1,64}");
    private static final int MAX_SESSION_ID = 128;

    private final LogStore store;

    public RequestLoggingFilter(LogStore store) {
        this.store = store;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || !VALID_REQUEST_ID.matcher(requestId).matches()) {
            requestId = UUID.randomUUID().toString();
        }
        String sessionId = request.getHeader(SESSION_ID_HEADER);
        if (sessionId != null && sessionId.length() > MAX_SESSION_ID) {
            sessionId = sessionId.substring(0, MAX_SESSION_ID);
        }
        RequestContext.set(new RequestContext.Values(requestId, sessionId, null));
        response.setHeader(REQUEST_ID_HEADER, requestId);

        boolean logged = !"OPTIONS".equals(request.getMethod()) && !request.getRequestURI().equals("/heartbeat");
        if (!logged) {
            try {
                chain.doFilter(request, response);
            } finally {
                RequestContext.clear();
            }
            return;
        }

        var cachedRequest = new ContentCachingRequestWrapper(request, LogBodies.MAX_CHARS);
        var cachedResponse = new ContentCachingResponseWrapper(response);
        long start = System.nanoTime();
        try {
            chain.doFilter(cachedRequest, cachedResponse);
        } finally {
            try {
                int millis = (int) ((System.nanoTime() - start) / 1_000_000);
                RequestContext.Values ctx = RequestContext.current().orElseThrow();
                int status = cachedResponse.getStatus();
                store.write(new LogEntry(Instant.now(), LogEntry.Kind.REQUEST, status >= 500 ? "ERROR" : "INFO",
                        ctx.requestId(), ctx.sessionId(), ctx.user(), request.getMethod(), request.getRequestURI(),
                        request.getQueryString(), status, millis, null, Thread.currentThread().getName(),
                        null, LogBodies.prepare(text(cachedRequest.getContentAsByteArray(), request.getContentType())),
                        LogBodies.prepare(text(cachedResponse.getContentAsByteArray(), cachedResponse.getContentType())), null));
            } catch (RuntimeException e) {
                System.err.println("[db-log] could not log request: " + e.getMessage());
            } finally {
                cachedResponse.copyBodyToResponse();
                RequestContext.clear();
            }
        }
    }

    /** Only textual bodies are logged. */
    private static String text(byte[] bytes, String contentType) {
        if (bytes.length == 0) {
            return null;
        }
        if (contentType != null && !contentType.contains("json") && !contentType.startsWith("text/")) {
            return "[" + bytes.length + " bytes of " + contentType + "]";
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
