package com.factoryos.modules.operations.dto;

import com.factoryos.modules.machine.domain.MachineStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class ResolveBreakdownRequest {

    @NotNull(message = "Downtime event ID is required")
    private UUID downtimeEventId;

    @NotBlank(message = "Resolution note is required")
    @Size(max = 2000, message = "Resolution note cannot exceed 2000 characters")
    private String resolutionNote;

    private MachineStatus targetMachineStatus = MachineStatus.IDLE;

    @NotNull(message = "Expected version is required for optimistic locking")
    private Long expectedVersion;

    public ResolveBreakdownRequest() {
    }

    public UUID getDowntimeEventId() {
        return downtimeEventId;
    }

    public void setDowntimeEventId(UUID downtimeEventId) {
        this.downtimeEventId = downtimeEventId;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    public void setResolutionNote(String resolutionNote) {
        this.resolutionNote = resolutionNote;
    }

    public MachineStatus getTargetMachineStatus() {
        return targetMachineStatus;
    }

    public void setTargetMachineStatus(MachineStatus targetMachineStatus) {
        this.targetMachineStatus = targetMachineStatus;
    }

    public Long getExpectedVersion() {
        return expectedVersion;
    }

    public void setExpectedVersion(Long expectedVersion) {
        this.expectedVersion = expectedVersion;
    }
}
