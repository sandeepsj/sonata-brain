package com.example.sonata_brain.common.context;

import org.slf4j.MDC;

import java.util.Map;
import java.util.Optional;

/**
 * Who and what the current request is (REQ-008): request id, the client's session id, and the user from the access
 * token. Held per thread, so any code handling the request can read it with {@link #current()}. Every value is
 * mirrored into the SLF4J MDC under the same keys, so log statements carry it too.
 *
 * <p>Set by {@code RequestLoggingFilter} at the start of a request and cleared when it ends; the user is added by
 * {@code JwtAuthenticationFilter}. {@link ContextCopyingTaskDecorator} carries it over to {@code @Async} threads.
 */
public final class RequestContext {

    public static final String REQUEST_ID = "requestId";
    public static final String SESSION_ID = "sessionId";
    public static final String USER = "user";

    private static final ThreadLocal<Values> CURRENT = new ThreadLocal<>();

    /** A snapshot of the context; any field may be null. */
    public record Values(String requestId, String sessionId, String user) {
    }

    private RequestContext() {
    }

    /** The current request's context, or empty outside a request. */
    public static Optional<Values> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static void set(Values values) {
        CURRENT.set(values);
        put(REQUEST_ID, values.requestId());
        put(SESSION_ID, values.sessionId());
        put(USER, values.user());
    }

    public static void setUser(String user) {
        Values now = CURRENT.get();
        set(new Values(now == null ? null : now.requestId(), now == null ? null : now.sessionId(), user));
    }

    public static void clear() {
        CURRENT.remove();
        MDC.remove(REQUEST_ID);
        MDC.remove(SESSION_ID);
        MDC.remove(USER);
    }

    /** Context and MDC of this thread, to hand over to another thread. */
    static Snapshot capture() {
        return new Snapshot(CURRENT.get(), MDC.getCopyOfContextMap());
    }

    record Snapshot(Values values, Map<String, String> mdc) {
        void restore() {
            if (values != null) {
                CURRENT.set(values);
            }
            if (mdc != null) {
                MDC.setContextMap(mdc);
            }
        }
    }

    private static void put(String key, String value) {
        if (value == null) {
            MDC.remove(key);
        } else {
            MDC.put(key, value);
        }
    }
}
