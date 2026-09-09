package com.factoryos.modules.tenant.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SwitchPlantRequest(
        @NotNull(message = "Plant ID is required")
        UUID plantId
) {
}
