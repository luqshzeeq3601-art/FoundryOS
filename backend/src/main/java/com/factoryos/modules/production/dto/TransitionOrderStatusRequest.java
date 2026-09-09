package com.factoryos.modules.production.dto;

import com.factoryos.modules.production.domain.ProductionOrderStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class TransitionOrderStatusRequest {

    @NotNull(message = "Target status is required")
    private ProductionOrderStatus targetStatus;

    @Size(max = 2000, message = "Closure note cannot exceed 2000 characters")
    private String closureNote;

    @NotNull(message = "Expected version is required for optimistic locking")
    private Long expectedVersion;

    public TransitionOrderStatusRequest() {
    }

    public ProductionOrderStatus getTargetStatus() {
        return targetStatus;
    }

    public void setTargetStatus(ProductionOrderStatus targetStatus) {
        this.targetStatus = targetStatus;
    }

    public String getClosureNote() {
        return closureNote;
    }

    public void setClosureNote(String closureNote) {
        this.closureNote = closureNote;
    }

    public Long getExpectedVersion() {
        return expectedVersion;
    }

    public void setExpectedVersion(Long expectedVersion) {
        this.expectedVersion = expectedVersion;
    }
}
