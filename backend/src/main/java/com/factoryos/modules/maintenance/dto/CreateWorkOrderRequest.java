package com.factoryos.modules.maintenance.dto;

import com.factoryos.modules.maintenance.domain.MaintenancePriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public class CreateWorkOrderRequest {

    @NotBlank(message = "Work order number is required")
    @Size(max = 40, message = "Work order number cannot exceed 40 characters")
    private String workOrderNumber;

    @NotNull(message = "Machine ID is required")
    private UUID machineId;

    private UUID downtimeEventId;

    @NotBlank(message = "Title is required")
    @Size(max = 160, message = "Title cannot exceed 160 characters")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(max = 4000, message = "Description cannot exceed 4000 characters")
    private String description;

    private MaintenancePriority priority = MaintenancePriority.MEDIUM;

    private UUID assignedTo;

    private Instant dueAt;

    public CreateWorkOrderRequest() {
    }

    public String getWorkOrderNumber() {
        return workOrderNumber;
    }

    public void setWorkOrderNumber(String workOrderNumber) {
        this.workOrderNumber = workOrderNumber;
    }

    public UUID getMachineId() {
        return machineId;
    }

    public void setMachineId(UUID machineId) {
        this.machineId = machineId;
    }

    public UUID getDowntimeEventId() {
        return downtimeEventId;
    }

    public void setDowntimeEventId(UUID downtimeEventId) {
        this.downtimeEventId = downtimeEventId;
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

    public UUID getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(UUID assignedTo) {
        this.assignedTo = assignedTo;
    }

    public Instant getDueAt() {
        return dueAt;
    }

    public void setDueAt(Instant dueAt) {
        this.dueAt = dueAt;
    }
}
