package com.example.sonata_brain.dues;

import com.example.sonata_brain.common.error.ApiException;
import com.example.sonata_brain.dues.DuesDtos.Changes;
import com.example.sonata_brain.dues.DuesDtos.Job;
import com.example.sonata_brain.fees.TimelineEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

/**
 * Background recalculation (REQ-074.AC5, AC8, AC9): on request, nightly, and once after the migration. Each student is
 * recalculated in its own transaction; a failure is recorded and the others go on. Jobs are kept in memory.
 */
@Component
public class DuesRebuildJobs {

    public enum Scope { ALL, STUDENT }

    private static final Logger log = LoggerFactory.getLogger(DuesRebuildJobs.class);

    private final DuesService dues;
    private final FeeDueRepository stored;
    private final TimelineEntryRepository timeline;
    private final Executor executor;
    private final Map<String, Job> jobs = new ConcurrentHashMap<>();

    public DuesRebuildJobs(DuesService dues, FeeDueRepository stored, TimelineEntryRepository timeline,
                           @Qualifier("duesEventExecutor") Executor executor) {
        this.dues = dues;
        this.stored = stored;
        this.timeline = timeline;
        this.executor = executor;
    }

    public Job start(Scope scope, Long studentId) {
        if (scope == Scope.STUDENT && studentId == null) {
            throw ApiException.invalidField("id", "id (the student's admission number) is required for scope STUDENT");
        }
        if (scope == Scope.STUDENT && !dues.exists(studentId)) {
            throw ApiException.invalidField("id", "Unknown student " + studentId);
        }
        String id = UUID.randomUUID().toString();
        Job job = new Job(id, scope.name(), "RUNNING", 0, new Changes(0, 0, 0), List.of(), LocalDateTime.now(), null);
        jobs.put(id, job);
        executor.execute(() -> run(job, scope == Scope.ALL ? dues.studentIds() : List.of(studentId)));
        return job;
    }

    public Job get(String jobId) {
        Job job = jobs.get(jobId);
        if (job == null) {
            throw ApiException.notFound("Rebuild job " + jobId + " not found");
        }
        return job;
    }

    private void run(Job started, List<Long> studentIds) {
        List<String> errors = new CopyOnWriteArrayList<>();
        Changes changes = new Changes(0, 0, 0);
        int done = 0;
        if (studentIds.size() > 1) {
            // Everyone at once: a few bulk queries and batched writes (fast even with a distant database).
            try {
                changes = dues.recalculateAll();
                done = studentIds.size();
            } catch (RuntimeException e) {
                log.error("Bulk recalculation failed; retrying student by student to find the problem", e);
            }
        }
        if (done == 0) {
            for (Long id : studentIds) {
                try {
                    changes = changes.plus(dues.recalculateStudent(id));
                } catch (RuntimeException e) {
                    log.error("Recalculating dues for student {} failed", id, e);
                    errors.add("Student " + id + ": " + e.getMessage());
                }
                done++;
            }
        }
        jobs.put(started.jobId(), new Job(started.jobId(), started.scope(), errors.isEmpty() || done > errors.size()
                ? "DONE" : "FAILED", done, changes, List.copyOf(errors), started.startedAt(), LocalDateTime.now()));
        log.info("Dues rebuild {} ({}) finished: {} students, {}, {} errors", started.jobId(), started.scope(), done,
                changes, errors.size());
    }

    /** REQ-074.AC9: new terms appear on their first day. */
    @Scheduled(cron = "${sonata.dues.nightly-cron:0 0 2 * * *}")
    public void nightly() {
        start(Scope.ALL, null);
    }

    /** REQ-082.AC5: after the migration, fill the empty projection once. */
    @EventListener(ApplicationReadyEvent.class)
    public void afterStartup() {
        if (stored.count() == 0 && timeline.count() > 0) {
            log.info("fee_due is empty and the timeline isn't: calculating everyone's dues");
            start(Scope.ALL, null);
        }
    }
}
