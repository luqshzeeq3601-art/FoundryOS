package com.factoryos.modules.downtime.domain;

import com.factoryos.modules.auth.domain.User;
import com.factoryos.modules.machine.domain.Machine;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "downtime_events")
public class DowntimeEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id", nullable = false)
    private Machine machine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plant_id")
    private com.factoryos.modules.tenant.domain.Plant plant;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", nullable = false, length = 32)
    private DowntimeReasonCode reasonCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_source", nullable = false, length = 32)
    private DowntimeTriggerSource triggerSource = DowntimeTriggerSource.MANUAL;

    @Column(name = "is_micro_stop", nullable = false)
    private boolean isMicroStop = false;

    @Column(name = "root_cause_prompted_at")
    private Instant rootCausePromptedAt;

    @Column(name = "root_cause_acknowledged_at")
    private Instant rootCauseAcknowledgedAt;

    @Column(length = 2000)
    private String description;

    @Column(name = "start_time", nullable = false)
    private Instant startTime = Instant.now();

    @Column(name = "end_time")
    private Instant endTime;

    @Column(name = "resolution_note", length = 2000)
    private String resolutionNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private User resolvedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Version
    @Column(nullable = false)
    private long version = 0L;

    public DowntimeEvent() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Machine getMachine() {
        return machine;
    }

    public void setMachine(Machine machine) {
        this.machine = machine;
    }

    public DowntimeReasonCode getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(DowntimeReasonCode reasonCode) {
        this.reasonCode = reasonCode;
    }

    public DowntimeTriggerSource getTriggerSource() {
        return triggerSource;
    }

    public void setTriggerSource(DowntimeTriggerSource triggerSource) {
        this.triggerSource = triggerSource != null ? triggerSource : DowntimeTriggerSource.MANUAL;
    }

    public boolean isMicroStop() {
        return isMicroStop;
    }

    public void setMicroStop(boolean microStop) {
        isMicroStop = microStop;
    }

    public Instant getRootCausePromptedAt() {
        return rootCausePromptedAt;
    }

    public void setRootCausePromptedAt(Instant rootCausePromptedAt) {
        this.rootCausePromptedAt = rootCausePromptedAt;
    }

    public Instant getRootCauseAcknowledgedAt() {
        return rootCauseAcknowledgedAt;
    }

    public void setRootCauseAcknowledgedAt(Instant rootCauseAcknowledgedAt) {
        this.rootCauseAcknowledgedAt = rootCauseAcknowledgedAt;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    public void setResolutionNote(String resolutionNote) {
        this.resolutionNote = resolutionNote;
    }

    public User getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(User resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public com.factoryos.modules.tenant.domain.Plant getPlant() {
        return plant;
    }

    public void setPlant(com.factoryos.modules.tenant.domain.Plant plant) {
        this.plant = plant;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }
}
