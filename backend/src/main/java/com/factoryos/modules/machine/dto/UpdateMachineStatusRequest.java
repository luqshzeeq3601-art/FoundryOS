package com.factoryos.modules.machine.dto;

import com.factoryos.modules.downtime.domain.DowntimeReasonCode;
import com.factoryos.modules.machine.domain.MachineStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UpdateMachineStatusRequest {

    @NotNull(message = "Target status is required")
    private MachineStatus status;

    private DowntimeReasonCode reasonCode;

    @Size(max = 2000, message = "Description must be at most 2000 characters")
    private String description;

    @NotNull(message = "expectedVersion is required for optimistic locking")
    private Long expectedVersion;

    public UpdateMachineStatusRequest() {
    }

    public MachineStatus getStatus() {
        return status;
    }

    public void setStatus(MachineStatus status) {
        this.status = status;
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

    public Long getExpectedVersion() {
        return expectedVersion;
    }

    public void setExpectedVersion(Long expectedVersion) {
        this.expectedVersion = expectedVersion;
    }
}
