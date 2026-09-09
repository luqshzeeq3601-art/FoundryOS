package com.factoryos.modules.downtime.dto;

import com.factoryos.modules.downtime.domain.DowntimeEvent;
import com.factoryos.modules.downtime.domain.DowntimeReasonCode;
import com.factoryos.modules.downtime.domain.DowntimeTriggerSource;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class DowntimeEventDto {
    private UUID id;
    private UUID machineId;
    private String machineName;
    private DowntimeReasonCode reasonCode;
    private DowntimeTriggerSource triggerSource;
    private boolean isMicroStop;
    private Instant rootCausePromptedAt;
    private Instant rootCauseAcknowledgedAt;
    private String description;
    private Instant startTime;
    private Instant endTime;
    private Long durationSeconds;
    private String resolutionNote;
    private UUID resolvedBy;
    private String resolverName;
    private Instant createdAt;
    private Instant updatedAt;
    private long version;

    public DowntimeEventDto() {
    }

    public static DowntimeEventDto from(DowntimeEvent event) {
        DowntimeEventDto dto = new DowntimeEventDto();
        dto.setId(event.getId());
        dto.setMachineId(event.getMachine().getId());
        dto.setMachineName(event.getMachine().getName());
        dto.setReasonCode(event.getReasonCode());
        dto.setTriggerSource(event.getTriggerSource());
        dto.setMicroStop(event.isMicroStop());
        dto.setRootCausePromptedAt(event.getRootCausePromptedAt());
        dto.setRootCauseAcknowledgedAt(event.getRootCauseAcknowledgedAt());
        dto.setDescription(event.getDescription());
        dto.setStartTime(event.getStartTime());
        dto.setEndTime(event.getEndTime());
        
        Instant start = event.getStartTime();
        Instant end = event.getEndTime() != null ? event.getEndTime() : Instant.now();
        if (start != null && end != null) {
            dto.setDurationSeconds(Math.max(0, Duration.between(start, end).toSeconds()));
        }

        dto.setResolutionNote(event.getResolutionNote());
        if (event.getResolvedBy() != null) {
            dto.setResolvedBy(event.getResolvedBy().getId());
            dto.setResolverName(event.getResolvedBy().getDisplayName());
        }
        dto.setCreatedAt(event.getCreatedAt());
        dto.setUpdatedAt(event.getUpdatedAt());
        dto.setVersion(event.getVersion());
        return dto;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getMachineId() {
        return machineId;
    }

    public void setMachineId(UUID machineId) {
        this.machineId = machineId;
    }

    public String getMachineName() {
        return machineName;
    }

    public void setMachineName(String machineName) {
        this.machineName = machineName;
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
        this.triggerSource = triggerSource;
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

    public Long getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    public void setResolutionNote(String resolutionNote) {
        this.resolutionNote = resolutionNote;
    }

    public UUID getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(UUID resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public String getResolverName() {
        return resolverName;
    }

    public void setResolverName(String resolverName) {
        this.resolverName = resolverName;
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

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }
}
