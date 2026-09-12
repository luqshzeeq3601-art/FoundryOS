package com.factoryos.modules.edge.dto;

import java.time.Instant;
import java.util.UUID;

public class EdgeGatewayDto {

    private UUID id;
    private String gatewayCode;
    private String name;
    private UUID plantId;
    private String plantName;
    private String ipAddress;
    private String macAddress;
    private String status;
    private Instant lastHeartbeatAt;
    private Instant lastSyncAt;
    private long lastSyncSequenceId;
    private int bufferCapacityRecords;
    private String firmwareVersion;
    private Instant createdAt;
    private Instant updatedAt;

    public EdgeGatewayDto() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public String getPlantName() {
        return plantName;
    }

    public void setPlantName(String plantName) {
        this.plantName = plantName;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getLastHeartbeatAt() {
        return lastHeartbeatAt;
    }

    public void setLastHeartbeatAt(Instant lastHeartbeatAt) {
        this.lastHeartbeatAt = lastHeartbeatAt;
    }

    public Instant getLastSyncAt() {
        return lastSyncAt;
    }

    public void setLastSyncAt(Instant lastSyncAt) {
        this.lastSyncAt = lastSyncAt;
    }

    public long getLastSyncSequenceId() {
        return lastSyncSequenceId;
    }

    public void setLastSyncSequenceId(long lastSyncSequenceId) {
        this.lastSyncSequenceId = lastSyncSequenceId;
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
}
