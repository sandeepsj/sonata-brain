package com.example.sonata_brain.common.logging;

import org.springframework.context.SmartLifecycle;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Writes log entries to the {@code log} table in the background (REQ-008.AC6). Callers only put the entry on a
 * bounded queue; one writer thread inserts batches in their own transactions, outside any request's transaction.
 * When the queue is full the entry is dropped, and database errors are reported on stderr: logging never fails a
 * request.
 */
@Component
public class DatabaseLogStore implements LogStore, SmartLifecycle {

    static final String WRITER_THREAD = "db-log-writer";

    private static final int CAPACITY = 10_000;
    private static final int BATCH = 200;
    private static final long FLUSH_MILLIS = 500;
    private static final String INSERT = """
            INSERT INTO log (created_at, kind, level, request_id, session_id, user_email, method, path, query, status,
                             duration_ms, logger, thread, message, request_body, response_body, stack_trace)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""";

    private final JdbcTemplate jdbc;
    private final BlockingQueue<LogEntry> queue = new ArrayBlockingQueue<>(CAPACITY);
    private final AtomicLong dropped = new AtomicLong();
    private volatile boolean running;
    private Thread writer;

    public DatabaseLogStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void write(LogEntry entry) {
        if (!queue.offer(entry)) {
            long n = dropped.incrementAndGet();
            if (n == 1 || n % 1000 == 0) {
                System.err.println("[db-log] queue full, " + n + " log entries dropped so far");
            }
        }
    }

    @Override
    public void start() {
        running = true;
        writer = new Thread(this::drainLoop, WRITER_THREAD);
        writer.setDaemon(true);
        writer.start();
    }

    @Override
    public void stop() {
        running = false;
        if (writer != null) {
            writer.interrupt();
            try {
                writer.join(5_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        flush(); // whatever is left
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private void drainLoop() {
        while (running) {
            try {
                LogEntry first = queue.poll(FLUSH_MILLIS, TimeUnit.MILLISECONDS);
                if (first != null) {
                    List<LogEntry> batch = new ArrayList<>(BATCH);
                    batch.add(first);
                    queue.drainTo(batch, BATCH - 1);
                    insert(batch);
                }
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    private void flush() {
        List<LogEntry> batch = new ArrayList<>(BATCH);
        while (queue.drainTo(batch, BATCH) > 0) {
            insert(batch);
            batch.clear();
        }
    }

    private void insert(List<LogEntry> batch) {
        try {
            jdbc.batchUpdate(INSERT, batch, batch.size(), (ps, e) -> {
                ps.setTimestamp(1, Timestamp.from(e.createdAt()));
                ps.setString(2, e.kind().name());
                ps.setString(3, e.level());
                ps.setString(4, e.requestId());
                ps.setString(5, e.sessionId());
                ps.setString(6, e.user());
                ps.setString(7, e.method());
                ps.setString(8, e.path());
                ps.setString(9, e.query());
                ps.setObject(10, e.status());
                ps.setObject(11, e.durationMs());
                ps.setString(12, e.logger());
                ps.setString(13, e.thread());
                ps.setString(14, e.message());
                ps.setString(15, e.requestBody());
                ps.setString(16, e.responseBody());
                ps.setString(17, e.stackTrace());
            });
        } catch (RuntimeException ex) {
            System.err.println("[db-log] could not write " + batch.size() + " log entries: " + ex.getMessage());
        }
    }
}
