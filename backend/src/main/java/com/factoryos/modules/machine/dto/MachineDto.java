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
    private Instant createdAt;
    private Instant updatedAt;
    private long version;

    public MachineDto() {
    }

    public static MachineDto from(Machine machine) {
        MachineDto dto = new MachineDto();
        dto.setId(machine.getId());
        dto.setSerialNumber(machine.getSerialNumber());
        dto.setName(machine.getName());
        dto.setLocation(machine.getLocation());
        dto.setDescription(machine.getDescription());
        dto.setStatus(machine.getStatus());
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
