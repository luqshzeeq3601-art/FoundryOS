package com.factoryos.modules.downtime.dto;

import com.factoryos.modules.downtime.domain.DowntimeReasonCode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class AcknowledgeRootCauseRequest {

    @NotNull(message = "Reason code is required")
    private DowntimeReasonCode reasonCode;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @NotNull(message = "Expected version is required")
    private Long expectedVersion;

    public AcknowledgeRootCauseRequest() {
    }

    public AcknowledgeRootCauseRequest(DowntimeReasonCode reasonCode, String description, Long expectedVersion) {
        this.reasonCode = reasonCode;
        this.description = description;
        this.expectedVersion = expectedVersion;
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
