package com.factoryos.modules.downtime.dto;

import com.factoryos.modules.downtime.domain.DowntimeReasonCode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public class CreateDowntimeRequest {

    @NotNull(message = "Machine ID is required")
    private UUID machineId;

    @NotNull(message = "Reason code is required")
    private DowntimeReasonCode reasonCode;

    @Size(max = 2000, message = "Description must be at most 2000 characters")
    private String description;

    private Instant startTime;

    private Long expectedMachineVersion;

    public CreateDowntimeRequest() {
    }

    public UUID getMachineId() {
        return machineId;
    }

    public void setMachineId(UUID machineId) {
        this.machineId = machineId;
    }

    public DowntimeReasonCode getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(DowntimeReasonCode reasonCode) {
        this.reasonCode = reasonCode;
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

    public Long getExpectedMachineVersion() {
        return expectedMachineVersion;
    }

    public void setExpectedMachineVersion(Long expectedMachineVersion) {
        this.expectedMachineVersion = expectedMachineVersion;
    }
}
