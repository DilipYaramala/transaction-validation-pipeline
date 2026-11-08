
package com.dilip.transaction.entity;

import jakarta.persistence.*;
import java.time.Instant;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "pipeline_run",
       uniqueConstraints = @UniqueConstraint(columnNames = "source_file"))
public class PipelineRun implements Persistable<String> {

    @Id
    @Column(name = "run_id", length = 36)
    private String runId;

    @Column(name = "source_file", nullable = false, unique = true)
    private String sourceFile;

    @Column(nullable = false)
    private String status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "input_count")
    private int inputCount;

    @Column(name = "curated_count")
    private int curatedCount;

    @Column(name = "rejected_count")
    private int rejectedCount;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Transient
    private boolean newEntity = true;

    protected PipelineRun() {}

    public PipelineRun(String id, String file) {
        this.runId = id;
        this.sourceFile = file;
        this.status = "RUNNING";
        this.startedAt = Instant.now();
    }

    @Override
    public String getId() {
        return runId;
    }

    @Override
    @Transient
    public boolean isNew() {
        return newEntity;
    }

    @PostLoad
    @PostPersist
    private void markNotNew() {
        newEntity = false;
    }

    public void complete(int i, int c, int r) {
        inputCount = i;
        curatedCount = c;
        rejectedCount = r;
        status = "COMPLETED";
        endedAt = Instant.now();
    }

    public void fail(String why) {
        status = "FAILED";
        failureReason = why;
        endedAt = Instant.now();
    }

    public void restart() {
        status = "RUNNING";
        startedAt = Instant.now();
        endedAt = null;
        inputCount = 0;
        curatedCount = 0;
        rejectedCount = 0;
        failureReason = null;
    }

    public String getRunId() { return runId; }
    public String getSourceFile() { return sourceFile; }
    public String getStatus() { return status; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getEndedAt() { return endedAt; }
    public int getInputCount() { return inputCount; }
    public int getCuratedCount() { return curatedCount; }
    public int getRejectedCount() { return rejectedCount; }
    public String getFailureReason() { return failureReason; }
}
