package com.factoryos.modules.tenant.dto;

import com.factoryos.modules.tenant.domain.Plant;

import java.time.Instant;
import java.util.UUID;

public record PlantDto(
        UUID id,
        UUID enterpriseId,
        String enterpriseName,
        String code,
        String name,
        String timezone,
        String address,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
    public static PlantDto from(Plant plant) {
        if (plant == null) return null;
        return new PlantDto(
                plant.getId(),
                plant.getEnterprise() != null ? plant.getEnterprise().getId() : null,
                plant.getEnterprise() != null ? plant.getEnterprise().getName() : null,
                plant.getCode(),
                plant.getName(),
                plant.getTimezone(),
                plant.getAddress(),
                plant.getStatus(),
                plant.getCreatedAt(),
                plant.getUpdatedAt()
        );
    }
}
