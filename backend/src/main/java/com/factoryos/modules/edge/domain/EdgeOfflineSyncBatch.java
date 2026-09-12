package com.factoryos.modules.edge.domain;

import com.factoryos.modules.tenant.domain.Plant;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "edge_offline_sync_batches")
public class EdgeOfflineSyncBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gateway_id", nullable = false)
    private EdgeGateway gateway;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plant_id", nullable = false)
    private Plant plant;

    @Column(name = "batch_id", nullable = false, unique = true, length = 80)
    private String batchId;

    @Column(name = "sequence_start", nullable = false)
    private long sequenceStart;

    @Column(name = "sequence_end", nullable = false)
    private long sequenceEnd;

    @Column(name = "total_records", nullable = false)
    private int totalRecords;

    @Column(name = "processed_records", nullable = false)
    private int processedRecords = 0;

    @Column(name = "failed_records", nullable = false)
    private int failedRecords = 0;

    @Column(name = "disconnected_at", nullable = false)
    private Instant disconnectedAt;

    @Column(name = "reconnected_at", nullable = false)
    private Instant reconnectedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_status", nullable = false, length = 20)
    private SyncStatus syncStatus = SyncStatus.PENDING;

    @Column(name = "reconciliation_notes", columnDefinition = "TEXT")
    private String reconciliationNotes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "reconciled_at")
    private Instant reconciledAt;

    public EdgeOfflineSyncBatch() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public long getSequenceStart() {
        return sequenceStart;
    }

    public void setSequenceStart(long sequenceStart) {
        this.sequenceStart = sequenceStart;
    }

    public long getSequenceEnd() {
        return sequenceEnd;
    }

    public void setSequenceEnd(long sequenceEnd) {
        this.sequenceEnd = sequenceEnd;
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

    public Instant getDisconnectedAt() {
        return disconnectedAt;
    }

    public void setDisconnectedAt(Instant disconnectedAt) {
        this.disconnectedAt = disconnectedAt;
    }

    public Instant getReconnectedAt() {
        return reconnectedAt;
    }

    public void setReconnectedAt(Instant reconnectedAt) {
        this.reconnectedAt = reconnectedAt;
    }

    public SyncStatus getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(SyncStatus syncStatus) {
        this.syncStatus = syncStatus;
    }

    public String getReconciliationNotes() {
        return reconciliationNotes;
    }

    public void setReconciliationNotes(String reconciliationNotes) {
        this.reconciliationNotes = reconciliationNotes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getReconciledAt() {
        return reconciledAt;
    }

    public void setReconciledAt(Instant reconciledAt) {
        this.reconciledAt = reconciledAt;
    }
}
