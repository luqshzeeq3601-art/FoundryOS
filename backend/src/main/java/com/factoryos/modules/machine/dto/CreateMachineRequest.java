package com.factoryos.modules.machine.dto;

import com.factoryos.modules.machine.domain.MachineStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateMachineRequest {

    @NotBlank(message = "Serial number is required")
    @Size(max = 80, message = "Serial number must be at most 80 characters")
    private String serialNumber;

    @NotBlank(message = "Name is required")
    @Size(max = 120, message = "Name must be at most 120 characters")
    private String name;

    @NotBlank(message = "Location is required")
    @Size(max = 160, message = "Location must be at most 160 characters")
    private String location;

    @Size(max = 2000, message = "Description must be at most 2000 characters")
    private String description;

    private MachineStatus status = MachineStatus.IDLE;

    public CreateMachineRequest() {
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
}
