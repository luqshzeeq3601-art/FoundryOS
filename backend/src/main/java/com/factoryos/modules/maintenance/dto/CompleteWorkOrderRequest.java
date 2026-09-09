package com.factoryos.modules.maintenance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CompleteWorkOrderRequest {

    @NotBlank(message = "Completion note is required")
    @Size(max = 4000, message = "Completion note cannot exceed 4000 characters")
    private String completionNote;

    @NotNull(message = "Expected version is required for optimistic locking")
    private Long expectedVersion;

    public CompleteWorkOrderRequest() {
    }

    public String getCompletionNote() {
        return completionNote;
    }

    public void setCompletionNote(String completionNote) {
        this.completionNote = completionNote;
    }

    public Long getExpectedVersion() {
        return expectedVersion;
    }

    public void setExpectedVersion(Long expectedVersion) {
        this.expectedVersion = expectedVersion;
    }
}
