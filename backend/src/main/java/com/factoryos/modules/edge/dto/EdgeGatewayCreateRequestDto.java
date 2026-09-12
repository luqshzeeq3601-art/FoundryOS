package com.factoryos.modules.edge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public class EdgeGatewayCreateRequestDto {

    @NotBlank(message = "Gateway code is required")
    @Size(max = 40, message = "Gateway code cannot exceed 40 characters")
    private String gatewayCode;

    @NotBlank(message = "Name is required")
    @Size(max = 120, message = "Name cannot exceed 120 characters")
    private String name;

    @NotNull(message = "Plant ID is required")
    private UUID plantId;

    @Size(max = 64)
    private String ipAddress;

    @Size(max = 64)
    private String macAddress;

    private int bufferCapacityRecords = 100000;

    @Size(max = 32)
    private String firmwareVersion = "2.0.0-EDGE";

    public EdgeGatewayCreateRequestDto() {
    }

    public String getGatewayCode() {
        return gatewayCode;
    }

    public void setGatewayCode(String gatewayCode) {
        this.gatewayCode = gatewayCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getPlantId() {
        return plantId;
    }

    public void setPlantId(UUID plantId) {
        this.plantId = plantId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public int getBufferCapacityRecords() {
        return bufferCapacityRecords;
    }

    public void setBufferCapacityRecords(int bufferCapacityRecords) {
        this.bufferCapacityRecords = bufferCapacityRecords;
    }

    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }
}
