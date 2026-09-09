package com.factoryos.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ResetPasswordRequest {

    @NotBlank(message = "Temporary password is required")
    @Size(min = 12, max = 72, message = "Temporary password must be between 12 and 72 characters")
    private String temporaryPassword;

    @NotNull(message = "expectedVersion is required")
    private Long expectedVersion;

    public ResetPasswordRequest() {
    }

    public String getTemporaryPassword() {
        return temporaryPassword;
    }

    public void setTemporaryPassword(String temporaryPassword) {
        this.temporaryPassword = temporaryPassword;
    }

    public Long getExpectedVersion() {
        return expectedVersion;
    }

    public void setExpectedVersion(Long expectedVersion) {
        this.expectedVersion = expectedVersion;
    }
}
