package com.factoryos.modules.downtime.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public class ResolveDowntimeRequest {

    @NotBlank(message = "Resolution note is required")
    @Size(max = 2000, message = "Resolution note must be at most 2000 characters")
    private String resolutionNote;

    private Instant endTime;

    @NotNull(message = "expectedVersion is required for downtime event")
    private Long expectedVersion;

    private Long expectedMachineVersion;

    public ResolveDowntimeRequest() {
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    public void setResolutionNote(String resolutionNote) {
        this.resolutionNote = resolutionNote;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public Long getExpectedVersion() {
        return expectedVersion;
    }

    public void setExpectedVersion(Long expectedVersion) {
        this.expectedVersion = expectedVersion;
    }

    public Long getExpectedMachineVersion() {
        return expectedMachineVersion;
    }

    public void setExpectedMachineVersion(Long expectedMachineVersion) {
        this.expectedMachineVersion = expectedMachineVersion;
    }
}
