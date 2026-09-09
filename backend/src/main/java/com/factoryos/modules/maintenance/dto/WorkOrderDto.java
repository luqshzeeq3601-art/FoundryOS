package com.factoryos.modules.maintenance.dto;

import com.factoryos.modules.maintenance.domain.MaintenancePriority;
import com.factoryos.modules.maintenance.domain.MaintenanceStatus;
import com.factoryos.modules.maintenance.domain.MaintenanceWorkOrder;

import java.time.Instant;
import java.util.UUID;

public class WorkOrderDto {
    private UUID id;
    private String workOrderNumber;
    private UUID machineId;
    private String machineName;
    private UUID downtimeEventId;
    private String title;
    private String description;
    private MaintenancePriority priority;
    private MaintenanceStatus status;
    private UUID assignedToId;
    private String assignedToName;
    private Instant dueAt;
    private Instant startedAt;
    private Instant completedAt;
    private Instant closedAt;
    private String completionNote;
    private String cancellationNote;
    private Instant createdAt;
    private Instant updatedAt;
    private long version;

    public WorkOrderDto() {
    }

    public static WorkOrderDto from(MaintenanceWorkOrder order) {
        WorkOrderDto dto = new WorkOrderDto();
        dto.setId(order.getId());
        dto.setWorkOrderNumber(order.getWorkOrderNumber());
        dto.setMachineId(order.getMachine().getId());
        dto.setMachineName(order.getMachine().getName());
        if (order.getDowntimeEvent() != null) {
            dto.setDowntimeEventId(order.getDowntimeEvent().getId());
        }
        dto.setTitle(order.getTitle());
        dto.setDescription(order.getDescription());
        dto.setPriority(order.getPriority());
        dto.setStatus(order.getStatus());
        if (order.getAssignedTo() != null) {
            dto.setAssignedToId(order.getAssignedTo().getId());
            dto.setAssignedToName(order.getAssignedTo().getDisplayName());
        }
        dto.setDueAt(order.getDueAt());
        dto.setStartedAt(order.getStartedAt());
        dto.setCompletedAt(order.getCompletedAt());
        dto.setClosedAt(order.getClosedAt());
        dto.setCompletionNote(order.getCompletionNote());
        dto.setCancellationNote(order.getCancellationNote());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());
        dto.setVersion(order.getVersion());
        return dto;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public String getMachineName() {
        return machineName;
    }

    public void setMachineName(String machineName) {
        this.machineName = machineName;
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

    public MaintenanceStatus getStatus() {
        return status;
    }

    public void setStatus(MaintenanceStatus status) {
        this.status = status;
    }

    public UUID getAssignedToId() {
        return assignedToId;
    }

    public void setAssignedToId(UUID assignedToId) {
        this.assignedToId = assignedToId;
    }

    public String getAssignedToName() {
        return assignedToName;
    }

    public void setAssignedToName(String assignedToName) {
        this.assignedToName = assignedToName;
    }

    public Instant getDueAt() {
        return dueAt;
    }

    public void setDueAt(Instant dueAt) {
        this.dueAt = dueAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    public String getCompletionNote() {
        return completionNote;
    }

    public void setCompletionNote(String completionNote) {
        this.completionNote = completionNote;
    }

    public String getCancellationNote() {
        return cancellationNote;
    }

    public void setCancellationNote(String cancellationNote) {
        this.cancellationNote = cancellationNote;
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
