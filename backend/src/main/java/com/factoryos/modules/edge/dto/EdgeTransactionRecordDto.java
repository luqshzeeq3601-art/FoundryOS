package com.factoryos.modules.edge.dto;

import com.factoryos.modules.edge.domain.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public class EdgeTransactionRecordDto {

    private long sequenceId;

    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;

    @NotNull(message = "Transaction type is required")
    private TransactionType transactionType;

    @NotBlank(message = "Entity type is required")
    private String entityType;

    private UUID entityId;

    @NotBlank(message = "Payload JSON is required")
    private String payloadJson;

    private long vectorClockVersion = 1L;

    @NotNull(message = "Recorded at timestamp is required")
    private Instant recordedAt;

    public EdgeTransactionRecordDto() {
    }

    public EdgeTransactionRecordDto(long sequenceId, String idempotencyKey, TransactionType transactionType,
                                    String entityType, UUID entityId, String payloadJson,
                                    long vectorClockVersion, Instant recordedAt) {
        this.sequenceId = sequenceId;
        this.idempotencyKey = idempotencyKey;
        this.transactionType = transactionType;
        this.entityType = entityType;
        this.entityId = entityId;
        this.payloadJson = payloadJson;
        this.vectorClockVersion = vectorClockVersion;
        this.recordedAt = recordedAt;
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
}
