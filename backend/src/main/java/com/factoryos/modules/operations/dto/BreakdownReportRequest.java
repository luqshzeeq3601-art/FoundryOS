package com.factoryos.modules.operations.dto;

import com.factoryos.modules.downtime.domain.DowntimeReasonCode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class BreakdownReportRequest {

    @NotNull(message = "Machine ID is required")
    private UUID machineId;

    @NotNull(message = "Reason code is required")
    private DowntimeReasonCode reasonCode;

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;

    private boolean createWorkOrder = false;

    @Size(max = 160, message = "Work order title cannot exceed 160 characters")
    private String workOrderTitle;

    public BreakdownReportRequest() {
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

    public boolean isCreateWorkOrder() {
        return createWorkOrder;
    }

    public void setCreateWorkOrder(boolean createWorkOrder) {
        this.createWorkOrder = createWorkOrder;
    }

    public String getWorkOrderTitle() {
        return workOrderTitle;
    }

    public void setWorkOrderTitle(String workOrderTitle) {
        this.workOrderTitle = workOrderTitle;
    }
}
