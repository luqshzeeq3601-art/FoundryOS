package com.factoryos.modules.tenant.dto;

import com.factoryos.modules.machine.dto.MachineDto;

import java.util.List;
import java.util.UUID;

public record HierarchyTreeDto(
        UUID plantId,
        String plantCode,
        String plantName,
        String timezone,
        String status,
        List<AreaNodeDto> areas
) {
    public record AreaNodeDto(
            UUID areaId,
            String areaCode,
            String areaName,
            List<LineNodeDto> lines
    ) {}

    public record LineNodeDto(
            UUID lineId,
            String lineCode,
            String lineName,
            List<WorkCellNodeDto> workCells
    ) {}

    public record WorkCellNodeDto(
            UUID cellId,
            String cellCode,
            String cellName,
            List<MachineDto> machines
    ) {}
}
