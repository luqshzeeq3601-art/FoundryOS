package com.factoryos.modules.edge.domain;

import com.factoryos.modules.tenant.domain.Plant;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "edge_offline_transaction_logs")
public class EdgeOfflineTransactionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private EdgeOfflineSyncBatch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gateway_id", nullable = false)
    private EdgeGateway gateway;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plant_id", nullable = false)
    private Plant plant;

    @Column(name = "sequence_id", nullable = false)
    private long sequenceId;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 120)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 40)
    private TransactionType transactionType;

    @Column(name = "entity_type", nullable = false, length = 40)
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
    private String payloadJson;

    @Column(name = "vector_clock_version", nullable = false)
    private long vectorClockVersion = 1L;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt = Instant.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "execution_status", nullable = false, length = 25)
    private ExecutionStatus executionStatus = ExecutionStatus.PROCESSED;

    @Column(name = "conflict_resolution_note", length = 1000)
    private String conflictResolutionNote;

    public EdgeOfflineTransactionLog() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public EdgeOfflineSyncBatch getBatch() {
        return batch;
    }

    public void setBatch(EdgeOfflineSyncBatch batch) {
        this.batch = batch;
    }

    public EdgeGateway getGateway() {
        return gateway;
    }

    public void setGateway(EdgeGateway gateway) {
        this.gateway = gateway;
    }

    public Plant getPlant() {
        return plant;
    }

    public void setPlant(Plant plant) {
        this.plant = plant;
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
