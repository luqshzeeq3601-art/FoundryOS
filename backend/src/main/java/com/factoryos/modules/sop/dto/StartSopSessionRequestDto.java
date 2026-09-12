package com.factoryos.modules.sop.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class StartSopSessionRequestDto {

    @NotNull(message = "Production Order ID is required")
    private UUID productionOrderId;

    private UUID sopId; // Optional: if omitted, auto-resolve from order's productCode
    private UUID machineId;

    public StartSopSessionRequestDto() {
    }

    public UUID getProductionOrderId() {
        return productionOrderId;
    }

    public void setProductionOrderId(UUID productionOrderId) {
        this.productionOrderId = productionOrderId;
    }

    public UUID getSopId() {
        return sopId;
    }

    public void setSopId(UUID sopId) {
        this.sopId = sopId;
    }

    public UUID getMachineId() {
        return machineId;
    }

    public void setMachineId(UUID machineId) {
        this.machineId = machineId;
    }
}
