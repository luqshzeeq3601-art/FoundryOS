package com.factoryos.modules.edge.dto;

import com.factoryos.modules.edge.domain.ExecutionStatus;
import com.factoryos.modules.edge.domain.TransactionType;
import java.time.Instant;
import java.util.UUID;

public class EdgeTransactionLogDto {

    private UUID id;
    private UUID batchId;
    private String batchCode;
    private String gatewayCode;
    private long sequenceId;
    private String idempotencyKey;
    private TransactionType transactionType;
    private String entityType;
    private UUID entityId;
    private String payloadJson;
    private long vectorClockVersion;
    private Instant recordedAt;
    private Instant syncedAt;
    private ExecutionStatus executionStatus;
    private String conflictResolutionNote;

    public EdgeTransactionLogDto() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getBatchId() {
        return batchId;
    }

    public void setBatchId(UUID batchId) {
        this.batchId = batchId;
    }

    public String getBatchCode() {
        return batchCode;
    }

    public void setBatchCode(String batchCode) {
        this.batchCode = batchCode;
    }

    public String getGatewayCode() {
        return gatewayCode;
    }

    public void setGatewayCode(String gatewayCode) {
        this.gatewayCode = gatewayCode;
    }

    public long getSequenceId() {
        return sequenceId;
    }

    public void setSequenceId(long sequenceId) {
        this.sequenceId = sequenceId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public void setEntityId(UUID entityId) {
        this.entityId = entityId;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }

    public long getVectorClockVersion() {
        return vectorClockVersion;
    }

    public void setVectorClockVersion(long vectorClockVersion) {
        this.vectorClockVersion = vectorClockVersion;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }

    public Instant getSyncedAt() {
        return syncedAt;
    }

    public void setSyncedAt(Instant syncedAt) {
        this.syncedAt = syncedAt;
    }

    public ExecutionStatus getExecutionStatus() {
        return executionStatus;
    }

    public void setExecutionStatus(ExecutionStatus executionStatus) {
        this.executionStatus = executionStatus;
    }

    public String getConflictResolutionNote() {
        return conflictResolutionNote;
    }

    public void setConflictResolutionNote(String conflictResolutionNote) {
        this.conflictResolutionNote = conflictResolutionNote;
    }
}
