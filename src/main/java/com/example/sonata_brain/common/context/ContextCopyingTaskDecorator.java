package com.example.sonata_brain.common.context;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

/**
 * Carries the request context (and MDC) from the thread that submits work to the thread that runs it, so
 * {@code @Async} work is still tagged with the request that started it (REQ-008.AC5).
 */
public class ContextCopyingTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable task) {
        RequestContext.Snapshot snapshot = RequestContext.capture();
        return () -> {
            snapshot.restore();
            try {
                task.run();
            } finally {
                RequestContext.clear();
                MDC.clear();
            }
        };
    }
}
