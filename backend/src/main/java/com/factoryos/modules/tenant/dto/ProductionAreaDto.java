package com.factoryos.modules.tenant.dto;

import com.factoryos.modules.tenant.domain.ProductionArea;

import java.time.Instant;
import java.util.UUID;

public record ProductionAreaDto(
        UUID id,
        UUID plantId,
        String code,
        String name,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProductionAreaDto from(ProductionArea area) {
        if (area == null) return null;
        return new ProductionAreaDto(
                area.getId(),
                area.getPlant() != null ? area.getPlant().getId() : null,
                area.getCode(),
                area.getName(),
                area.getDescription(),
                area.getCreatedAt(),
                area.getUpdatedAt()
        );
    }
}
