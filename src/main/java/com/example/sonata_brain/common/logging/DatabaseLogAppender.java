package com.example.sonata_brain.common.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.AppenderBase;
import com.example.sonata_brain.common.context.RequestContext;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Sends log statements to the {@link LogStore} as APP rows, or ERROR rows when an exception is attached (REQ-008.AC5).
 * Takes the application's own loggers at the configured level and above, and ERROR from any logger. The request
 * context comes from the event's MDC, so rows carry the request id, session id and user.
 */
@Component
public class DatabaseLogAppender extends AppenderBase<ILoggingEvent> {

    private static final String APP_PACKAGE = "com.example.sonata_brain.";
    private static final String OWN_PACKAGE = "com.example.sonata_brain.common.logging.";

    private final LogStore store;
    private final Level appLevel;

    public DatabaseLogAppender(LogStore store, @Value("${sonata.log.db.level:INFO}") String appLevel) {
        this.store = store;
        this.appLevel = Level.toLevel(appLevel, Level.INFO);
        setName("database");
    }

    /** Attach to the root logger once Spring (and Logback's own configuration) is up. */
    @EventListener(ApplicationStartedEvent.class)
    public void attach() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        setContext(context);
        start();
        Logger root = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        if (root.getAppender(getName()) == null) {
            root.addAppender(this);
        }
    }

    @Override
    protected void append(ILoggingEvent event) {
        String logger = event.getLoggerName();
        if (logger.startsWith(OWN_PACKAGE) || DatabaseLogStore.WRITER_THREAD.equals(event.getThreadName())) {
            return; // never log about logging
        }
        boolean ours = logger.startsWith(APP_PACKAGE) || logger.startsWith("app.");
        if (!(ours && event.getLevel().isGreaterOrEqual(appLevel)) && !event.getLevel().isGreaterOrEqual(Level.ERROR)) {
            return;
        }
        Map<String, String> mdc = event.getMDCPropertyMap();
        IThrowableProxy thrown = event.getThrowableProxy();
        store.write(new LogEntry(event.getInstant(), thrown != null ? LogEntry.Kind.ERROR : LogEntry.Kind.APP,
                event.getLevel().toString(), mdc.get(RequestContext.REQUEST_ID), mdc.get(RequestContext.SESSION_ID),
                mdc.get(RequestContext.USER), null, null, null, null, null, logger, event.getThreadName(),
                event.getFormattedMessage(), null, null, thrown != null ? ThrowableProxyUtil.asString(thrown) : null));
    }
}
