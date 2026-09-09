package com.factoryos.modules.telemetry.dto;

import java.time.Instant;

public class RetentionExecutionReport {

    private int rawPointsPruned;
    private int rollups1mPruned;
    private int rollups1hPruned;
    private long executionTimeMs;
    private Instant executedAt;

    public RetentionExecutionReport() {
        this.executedAt = Instant.now();
    }

    public RetentionExecutionReport(int rawPointsPruned, int rollups1mPruned, int rollups1hPruned, long executionTimeMs) {
        this.rawPointsPruned = rawPointsPruned;
        this.rollups1mPruned = rollups1mPruned;
        this.rollups1hPruned = rollups1hPruned;
        this.executionTimeMs = executionTimeMs;
        this.executedAt = Instant.now();
    }

    public int getRawPointsPruned() {
        return rawPointsPruned;
    }

    public void setRawPointsPruned(int rawPointsPruned) {
        this.rawPointsPruned = rawPointsPruned;
    }

    public int getRollups1mPruned() {
        return rollups1mPruned;
    }

    public void setRollups1mPruned(int rollups1mPruned) {
        this.rollups1mPruned = rollups1mPruned;
    }

    public int getRollups1hPruned() {
        return rollups1hPruned;
    }

    public void setRollups1hPruned(int rollups1hPruned) {
        this.rollups1hPruned = rollups1hPruned;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public Instant getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(Instant executedAt) {
        this.executedAt = executedAt;
    }
}
