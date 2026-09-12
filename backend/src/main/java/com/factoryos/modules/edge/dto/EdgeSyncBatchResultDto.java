package com.factoryos.modules.edge.dto;

import com.factoryos.modules.edge.domain.SyncStatus;
import java.time.Instant;
import java.util.UUID;

public class EdgeSyncBatchResultDto {

    private UUID id;
    private String batchId;
    private String gatewayCode;
    private SyncStatus syncStatus;
    private int totalRecords;
    private int processedRecords;
    private int failedRecords;
    private int duplicateIgnoredRecords;
    private int conflictResolvedRecords;
    private String reconciliationNotes;
    private Instant reconciledAt;

    public EdgeSyncBatchResultDto() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public String getGatewayCode() {
        return gatewayCode;
    }

    public void setGatewayCode(String gatewayCode) {
        this.gatewayCode = gatewayCode;
    }

    public SyncStatus getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(SyncStatus syncStatus) {
        this.syncStatus = syncStatus;
    }

    public int getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(int totalRecords) {
        this.totalRecords = totalRecords;
    }

    public int getProcessedRecords() {
        return processedRecords;
    }

    public void setProcessedRecords(int processedRecords) {
        this.processedRecords = processedRecords;
    }

    public int getFailedRecords() {
        return failedRecords;
    }

    public void setFailedRecords(int failedRecords) {
        this.failedRecords = failedRecords;
    }

    public int getDuplicateIgnoredRecords() {
        return duplicateIgnoredRecords;
    }

    public void setDuplicateIgnoredRecords(int duplicateIgnoredRecords) {
        this.duplicateIgnoredRecords = duplicateIgnoredRecords;
    }

    public int getConflictResolvedRecords() {
        return conflictResolvedRecords;
    }

    public void setConflictResolvedRecords(int conflictResolvedRecords) {
        this.conflictResolvedRecords = conflictResolvedRecords;
    }

    public String getReconciliationNotes() {
        return reconciliationNotes;
    }

    public void setReconciliationNotes(String reconciliationNotes) {
        this.reconciliationNotes = reconciliationNotes;
    }

    public Instant getReconciledAt() {
        return reconciledAt;
    }

    public void setReconciledAt(Instant reconciledAt) {
        this.reconciledAt = reconciledAt;
    }
}
