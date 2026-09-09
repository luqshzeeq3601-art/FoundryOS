package com.factoryos.modules.machine.dto;

import com.factoryos.modules.machine.domain.Machine;
import com.factoryos.modules.machine.domain.MachineStatus;

import java.time.Instant;
import java.util.UUID;

public class MachineDto {
    private UUID id;
    private String serialNumber;
    private String name;
    private String location;
    private String description;
    private MachineStatus status;
    private UUID plantId;
    private String plantCode;
    private String plantName;
    private UUID areaId;
    private String areaCode;
    private String areaName;
    private UUID lineId;
    private String lineCode;
    private String lineName;
    private UUID workCellId;
    private String workCellCode;
    private String workCellName;
    private Instant createdAt;
    private Instant updatedAt;
    private long version;

    public MachineDto() {
    }

    public static MachineDto from(Machine machine) {
        if (machine == null) return null;
        MachineDto dto = new MachineDto();
        dto.setId(machine.getId());
        dto.setSerialNumber(machine.getSerialNumber());
        dto.setName(machine.getName());
        dto.setLocation(machine.getLocation());
        dto.setDescription(machine.getDescription());
        dto.setStatus(machine.getStatus());
        if (machine.getPlant() != null) {
            dto.setPlantId(machine.getPlant().getId());
            dto.setPlantCode(machine.getPlant().getCode());
            dto.setPlantName(machine.getPlant().getName());
        }
        if (machine.getArea() != null) {
            dto.setAreaId(machine.getArea().getId());
            dto.setAreaCode(machine.getArea().getCode());
            dto.setAreaName(machine.getArea().getName());
        }
        if (machine.getLine() != null) {
            dto.setLineId(machine.getLine().getId());
            dto.setLineCode(machine.getLine().getCode());
            dto.setLineName(machine.getLine().getName());
        }
        if (machine.getWorkCell() != null) {
            dto.setWorkCellId(machine.getWorkCell().getId());
            dto.setWorkCellCode(machine.getWorkCell().getCode());
            dto.setWorkCellName(machine.getWorkCell().getName());
        }
        dto.setCreatedAt(machine.getCreatedAt());
        dto.setUpdatedAt(machine.getUpdatedAt());
        dto.setVersion(machine.getVersion());
        return dto;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
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

    public MachineStatus getStatus() {
        return status;
    }

    public void setStatus(MachineStatus status) {
        this.status = status;
    }

    public UUID getPlantId() {
        return plantId;
    }

    public void setPlantId(UUID plantId) {
        this.plantId = plantId;
    }

    public String getPlantCode() {
        return plantCode;
    }

    public void setPlantCode(String plantCode) {
        this.plantCode = plantCode;
    }

    public String getPlantName() {
        return plantName;
    }

    public void setPlantName(String plantName) {
        this.plantName = plantName;
    }

    public UUID getAreaId() {
        return areaId;
    }

    public void setAreaId(UUID areaId) {
        this.areaId = areaId;
    }

    public String getAreaCode() {
        return areaCode;
    }

    public void setAreaCode(String areaCode) {
        this.areaCode = areaCode;
    }

    public String getAreaName() {
        return areaName;
    }

    public void setAreaName(String areaName) {
        this.areaName = areaName;
    }

    public UUID getLineId() {
        return lineId;
    }

    public void setLineId(UUID lineId) {
        this.lineId = lineId;
    }

    public String getLineCode() {
        return lineCode;
    }

    public void setLineCode(String lineCode) {
        this.lineCode = lineCode;
    }

    public String getLineName() {
        return lineName;
    }

    public void setLineName(String lineName) {
        this.lineName = lineName;
    }

    public UUID getWorkCellId() {
        return workCellId;
    }

    public void setWorkCellId(UUID workCellId) {
        this.workCellId = workCellId;
    }

    public String getWorkCellCode() {
        return workCellCode;
    }

    public void setWorkCellCode(String workCellCode) {
        this.workCellCode = workCellCode;
    }

    public String getWorkCellName() {
        return workCellName;
    }

    public void setWorkCellName(String workCellName) {
        this.workCellName = workCellName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }
}
