package com.factoryos.modules.telemetry.dto;

import com.factoryos.modules.machine.domain.MachineStatus;
import com.factoryos.modules.telemetry.domain.ProtocolType;
import java.time.Instant;
import java.util.UUID;

public class MachineLiveTelemetryDto {

    private UUID machineId;
    private String machineName;
    private String serialNumber;
    private MachineStatus machineStatus;

    // Real-time sensor metrics
    private double spindleSpeedRpm;
    private double vibrationMmPerSec;
    private double motorCurrentAmps;
    private double bearingTempCelsius;

    // Health & diagnostic indicators
    private int healthScore; // 0 - 100%
    private ProtocolType activeProtocol;
    private String connectionStatus; // ONLINE, WARNING, CRITICAL, OFFLINE
    private Instant lastHeartbeat;
    private int configuredTagsCount;

    public MachineLiveTelemetryDto() {
    }

    public UUID getMachineId() {
        return machineId;
    }

    public void setMachineId(UUID machineId) {
        this.machineId = machineId;
    }

    public String getMachineName() {
        return machineName;
    }

    public void setMachineName(String machineName) {
        this.machineName = machineName;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public MachineStatus getMachineStatus() {
        return machineStatus;
    }

    public void setMachineStatus(MachineStatus machineStatus) {
        this.machineStatus = machineStatus;
    }

    public double getSpindleSpeedRpm() {
        return spindleSpeedRpm;
    }

    public void setSpindleSpeedRpm(double spindleSpeedRpm) {
        this.spindleSpeedRpm = spindleSpeedRpm;
    }

    public double getVibrationMmPerSec() {
        return vibrationMmPerSec;
    }

    public void setVibrationMmPerSec(double vibrationMmPerSec) {
        this.vibrationMmPerSec = vibrationMmPerSec;
    }

    public double getMotorCurrentAmps() {
        return motorCurrentAmps;
    }

    public void setMotorCurrentAmps(double motorCurrentAmps) {
        this.motorCurrentAmps = motorCurrentAmps;
    }

    public double getBearingTempCelsius() {
        return bearingTempCelsius;
    }

    public void setBearingTempCelsius(double bearingTempCelsius) {
        this.bearingTempCelsius = bearingTempCelsius;
    }

    public int getHealthScore() {
        return healthScore;
    }

    public void setHealthScore(int healthScore) {
        this.healthScore = healthScore;
    }

    public ProtocolType getActiveProtocol() {
        return activeProtocol;
    }

    public void setActiveProtocol(ProtocolType activeProtocol) {
        this.activeProtocol = activeProtocol;
    }

    public String getConnectionStatus() {
        return connectionStatus;
    }

    public void setConnectionStatus(String connectionStatus) {
        this.connectionStatus = connectionStatus;
    }

    public Instant getLastHeartbeat() {
        return lastHeartbeat;
    }

    public void setLastHeartbeat(Instant lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
    }

    public int getConfiguredTagsCount() {
        return configuredTagsCount;
    }

    public void setConfiguredTagsCount(int configuredTagsCount) {
        this.configuredTagsCount = configuredTagsCount;
    }
}
