package com.factoryos.modules.maintenance.dto;

import com.factoryos.modules.maintenance.domain.MaintenancePriority;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public class UpdateWorkOrderRequest {

    @Size(max = 160, message = "Title cannot exceed 160 characters")
    private String title;

    @Size(max = 4000, message = "Description cannot exceed 4000 characters")
    private String description;

    private MaintenancePriority priority;

    private Instant dueAt;

    @NotNull(message = "Expected version is required for optimistic locking")
    private Long expectedVersion;

    public UpdateWorkOrderRequest() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public MaintenancePriority getPriority() {
        return priority;
    }

    public void setPriority(MaintenancePriority priority) {
        this.priority = priority;
    }

    public Instant getDueAt() {
        return dueAt;
    }

    public void setDueAt(Instant dueAt) {
        this.dueAt = dueAt;
    }

    public Long getExpectedVersion() {
        return expectedVersion;
    }

    public void setExpectedVersion(Long expectedVersion) {
        this.expectedVersion = expectedVersion;
    }
}
