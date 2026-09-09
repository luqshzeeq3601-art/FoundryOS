package com.factoryos.modules.auth.dto;

import com.factoryos.modules.auth.domain.RoleType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UpdateUserRequest {

    @Size(max = 120, message = "Display name must be at most 120 characters")
    private String displayName;

    private RoleType role;

    private Boolean isActive;

    @NotNull(message = "expectedVersion is required for optimistic locking")
    private Long expectedVersion;

    public UpdateUserRequest() {
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public RoleType getRole() {
        return role;
    }

    public void setRole(RoleType role) {
        this.role = role;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public Long getExpectedVersion() {
        return expectedVersion;
    }

    public void setExpectedVersion(Long expectedVersion) {
        this.expectedVersion = expectedVersion;
    }
}
