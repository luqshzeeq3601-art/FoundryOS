package com.factoryos.modules.tenant.dto;

import com.factoryos.modules.tenant.domain.ProductionLine;

import java.time.Instant;
import java.util.UUID;

public record ProductionLineDto(
        UUID id,
        UUID areaId,
        String code,
        String name,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProductionLineDto from(ProductionLine line) {
        if (line == null) return null;
        return new ProductionLineDto(
                line.getId(),
                line.getArea() != null ? line.getArea().getId() : null,
                line.getCode(),
                line.getName(),
                line.getDescription(),
                line.getCreatedAt(),
                line.getUpdatedAt()
        );
    }
}
