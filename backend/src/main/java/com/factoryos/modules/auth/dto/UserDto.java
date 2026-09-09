package com.factoryos.modules.auth.dto;

import com.factoryos.modules.auth.domain.RoleType;
import com.factoryos.modules.auth.domain.User;
import com.factoryos.modules.tenant.dto.UserPlantMembershipDto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class UserDto {
    private UUID id;
    private String email;
    private String displayName;
    private RoleType role;
    private boolean isActive;
    private boolean mustChangePassword;
    private UUID activePlantId;
    private String activePlantCode;
    private String activePlantName;
    private String plantRole;
    private List<UserPlantMembershipDto> authorizedPlants;
    private Instant createdAt;
    private Instant updatedAt;
    private long version;

    public UserDto() {
    }

    public static UserDto from(User user) {
        if (user == null) return null;
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setDisplayName(user.getDisplayName());
        if (user.getRole() != null && user.getRole().getName() != null) {
            dto.setRole(user.getRole().getName());
        }
        dto.setActive(user.isActive());
        dto.setMustChangePassword(user.isMustChangePassword());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        dto.setVersion(user.getVersion());
        return dto;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }

    public UUID getActivePlantId() {
        return activePlantId;
    }

    public void setActivePlantId(UUID activePlantId) {
        this.activePlantId = activePlantId;
    }

    public String getActivePlantCode() {
        return activePlantCode;
    }

    public void setActivePlantCode(String activePlantCode) {
        this.activePlantCode = activePlantCode;
    }

    public String getActivePlantName() {
        return activePlantName;
    }

    public void setActivePlantName(String activePlantName) {
        this.activePlantName = activePlantName;
    }

    public String getPlantRole() {
        return plantRole;
    }

    public void setPlantRole(String plantRole) {
        this.plantRole = plantRole;
    }

    public List<UserPlantMembershipDto> getAuthorizedPlants() {
        return authorizedPlants;
    }

    public void setAuthorizedPlants(List<UserPlantMembershipDto> authorizedPlants) {
        this.authorizedPlants = authorizedPlants;
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
