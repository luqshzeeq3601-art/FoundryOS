package com.factoryos.modules.tenant.dto;

import com.factoryos.modules.tenant.domain.Enterprise;

import java.time.Instant;
import java.util.UUID;

public record EnterpriseDto(
        UUID id,
        String code,
        String name,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
    public static EnterpriseDto from(Enterprise enterprise) {
        if (enterprise == null) return null;
        return new EnterpriseDto(
                enterprise.getId(),
                enterprise.getCode(),
                enterprise.getName(),
                enterprise.getDescription(),
                enterprise.getCreatedAt(),
                enterprise.getUpdatedAt()
        );
    }
}
