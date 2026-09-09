package com.factoryos.modules.tenant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateAreaRequest(
        @NotNull(message = "Plant ID is required")
        UUID plantId,

        @NotBlank(message = "Area code is required")
        @Size(max = 40, message = "Code must not exceed 40 characters")
        @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Code must be alphanumeric with hyphens or underscores")
        String code,

        @NotBlank(message = "Area name is required")
        @Size(max = 120, message = "Name must not exceed 120 characters")
        String name,

        @Size(max = 2000, message = "Description must not exceed 2000 characters")
        String description
) {
}
