package com.example.sonata_brain.dues;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** A record of one requested rebuild and its outcome (REQ-074.AC3). Errors and changes are stored as JSON text. */
@Entity
@Table(name = "dues_rebuild_job")
public class DuesRebuildJob {

    public enum Scope { ALL, COURSE, STUDENT }

    public enum Status { RUNNING, DONE, FAILED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Scope scope;

    @Column(name = "scope_id")
    private String scopeId;

    @Column(name = "dry_run", nullable = false)
    private boolean dryRun;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.RUNNING;

    private int created;
    private int updated;
    private int deleted;

    @Column(nullable = false)
    private String errors = "[]";

    @Column(nullable = false)
    private String changes = "[]";

    @Column(name = "triggered_by")
    private String triggeredBy;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    protected DuesRebuildJob() {
    }

    public DuesRebuildJob(Scope scope, String scopeId, boolean dryRun, String triggeredBy) {
        this.scope = scope;
        this.scopeId = scopeId;
        this.dryRun = dryRun;
        this.triggeredBy = triggeredBy;
        this.startedAt = LocalDateTime.now();
    }

    public void finish(Status status, int created, int updated, int deleted, String errorsJson, String changesJson) {
        this.status = status;
        this.created = created;
        this.updated = updated;
        this.deleted = deleted;
        this.errors = errorsJson;
        this.changes = changesJson;
        this.finishedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Scope getScope() { return scope; }
    public String getScopeId() { return scopeId; }
    public boolean isDryRun() { return dryRun; }
    public Status getStatus() { return status; }
    public int getCreated() { return created; }
    public int getUpdated() { return updated; }
    public int getDeleted() { return deleted; }
    public String getErrors() { return errors; }
    public String getChanges() { return changes; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
}
