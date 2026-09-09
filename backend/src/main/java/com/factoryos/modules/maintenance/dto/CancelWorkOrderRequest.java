package com.factoryos.modules.maintenance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CancelWorkOrderRequest {

    @NotBlank(message = "Cancellation note is required")
    @Size(max = 2000, message = "Cancellation note cannot exceed 2000 characters")
    private String cancellationNote;

    @NotNull(message = "Expected version is required for optimistic locking")
    private Long expectedVersion;

    public CancelWorkOrderRequest() {
    }

    public String getCancellationNote() {
        return cancellationNote;
    }

    public void setCancellationNote(String cancellationNote) {
        this.cancellationNote = cancellationNote;
    }

    public Long getExpectedVersion() {
        return expectedVersion;
    }

    public void setExpectedVersion(Long expectedVersion) {
        this.expectedVersion = expectedVersion;
    }
}
