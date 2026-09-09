package com.factoryos.modules.tenant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreatePlantRequest(
        @NotNull(message = "Enterprise ID is required")
        UUID enterpriseId,

        @NotBlank(message = "Plant code is required")
        @Size(max = 40, message = "Code must not exceed 40 characters")
        @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Code must be alphanumeric with hyphens or underscores")
        String code,

        @NotBlank(message = "Plant name is required")
        @Size(max = 120, message = "Name must not exceed 120 characters")
        String name,

        @Size(max = 64, message = "Timezone must not exceed 64 characters")
        String timezone,

        @Size(max = 255, message = "Address must not exceed 255 characters")
        String address
) {
}
