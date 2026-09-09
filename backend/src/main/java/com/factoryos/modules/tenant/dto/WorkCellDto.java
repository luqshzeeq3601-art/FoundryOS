package com.factoryos.modules.tenant.dto;

import com.factoryos.modules.tenant.domain.WorkCell;

import java.time.Instant;
import java.util.UUID;

public record WorkCellDto(
        UUID id,
        UUID lineId,
        String code,
        String name,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
    public static WorkCellDto from(WorkCell cell) {
        if (cell == null) return null;
        return new WorkCellDto(
                cell.getId(),
                cell.getLine() != null ? cell.getLine().getId() : null,
                cell.getCode(),
                cell.getName(),
                cell.getDescription(),
                cell.getCreatedAt(),
                cell.getUpdatedAt()
        );
    }
}
