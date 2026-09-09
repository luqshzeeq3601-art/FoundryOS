package com.factoryos.modules.tenant.context;

import java.util.List;
import java.util.UUID;

public record TenantContext(
        UUID enterpriseId,
        UUID currentPlantId,
        String currentPlantCode,
        String currentPlantName,
        String currentPlantRole,
        boolean isGlobalAdmin,
        List<UUID> authorizedPlantIds
) {
    public boolean hasPlantAccess(UUID plantId) {
        if (isGlobalAdmin) {
            return true;
        }
        return plantId != null && authorizedPlantIds != null && authorizedPlantIds.contains(plantId);
    }
}
