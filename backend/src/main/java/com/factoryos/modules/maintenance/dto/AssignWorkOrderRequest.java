package com.factoryos.modules.maintenance.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class AssignWorkOrderRequest {

    @NotNull(message = "Technician user ID is required")
    private UUID assignedTo;

    @NotNull(message = "Expected version is required for optimistic locking")
    private Long expectedVersion;

    public AssignWorkOrderRequest() {
    }

    public UUID getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(UUID assignedTo) {
        this.assignedTo = assignedTo;
    }

    public Long getExpectedVersion() {
        return expectedVersion;
    }

    public void setExpectedVersion(Long expectedVersion) {
        this.expectedVersion = expectedVersion;
    }
}
