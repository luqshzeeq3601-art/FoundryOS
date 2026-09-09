package com.factoryos.modules.machine.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class UpdateMachineRequest {

    @Size(max = 120, message = "Name must be at most 120 characters")
    private String name;

    @Size(max = 160, message = "Location must be at most 160 characters")
    private String location;

    @Size(max = 2000, message = "Description must be at most 2000 characters")
    private String description;

    private UUID plantId;
    private UUID areaId;
    private UUID lineId;
    private UUID workCellId;

    @NotNull(message = "expectedVersion is required for optimistic locking")
    private Long expectedVersion;

    public UpdateMachineRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public UUID getPlantId() {
        return plantId;
    }

    public void setPlantId(UUID plantId) {
        this.plantId = plantId;
    }

    public UUID getAreaId() {
        return areaId;
    }

    public void setAreaId(UUID areaId) {
        this.areaId = areaId;
    }

    public UUID getLineId() {
        return lineId;
    }

    public void setLineId(UUID lineId) {
        this.lineId = lineId;
    }

    public UUID getWorkCellId() {
        return workCellId;
    }

    public void setWorkCellId(UUID workCellId) {
        this.workCellId = workCellId;
    }

    public Long getExpectedVersion() {
        return expectedVersion;
    }

    public void setExpectedVersion(Long expectedVersion) {
        this.expectedVersion = expectedVersion;
    }
}
