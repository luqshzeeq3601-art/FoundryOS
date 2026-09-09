package com.factoryos.modules.tenant.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AssignPlantMembershipRequest(
        @NotNull(message = "Plant ID is required")
        UUID plantId,

        @NotNull(message = "Role ID is required")
        UUID roleId,

        boolean isDefault
) {
}
