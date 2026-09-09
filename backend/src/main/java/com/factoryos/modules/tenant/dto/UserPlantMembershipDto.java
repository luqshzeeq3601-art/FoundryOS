package com.factoryos.modules.tenant.dto;

import com.factoryos.modules.tenant.domain.UserPlantMembership;

import java.time.Instant;
import java.util.UUID;

public record UserPlantMembershipDto(
        UUID id,
        UUID userId,
        String userEmail,
        String userDisplayName,
        UUID plantId,
        String plantCode,
        String plantName,
        UUID roleId,
        String roleName,
        boolean isDefault,
        Instant createdAt
) {
    public static UserPlantMembershipDto from(UserPlantMembership membership) {
        if (membership == null) return null;
        return new UserPlantMembershipDto(
                membership.getId(),
                membership.getUser() != null ? membership.getUser().getId() : null,
                membership.getUser() != null ? membership.getUser().getEmail() : null,
                membership.getUser() != null ? membership.getUser().getDisplayName() : null,
                membership.getPlant() != null ? membership.getPlant().getId() : null,
                membership.getPlant() != null ? membership.getPlant().getCode() : null,
                membership.getPlant() != null ? membership.getPlant().getName() : null,
                membership.getRole() != null ? membership.getRole().getId() : null,
                membership.getRole() != null ? membership.getRole().getName().name() : null,
                membership.isDefault(),
                membership.getCreatedAt()
        );
    }
}
