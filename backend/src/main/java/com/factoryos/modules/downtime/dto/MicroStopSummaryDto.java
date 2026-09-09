package com.factoryos.modules.downtime.dto;

import java.util.UUID;

public class MicroStopSummaryDto {
    private UUID machineId;
    private String machineName;
    private long microStopCount;
    private long totalMicroStopSeconds;
    private long majorDowntimeCount;
    private long totalMajorDowntimeSeconds;
    private double microStopPercentage;

    public MicroStopSummaryDto() {
    }

    public MicroStopSummaryDto(
            UUID machineId,
            String machineName,
            long microStopCount,
            long totalMicroStopSeconds,
            long majorDowntimeCount,
            long totalMajorDowntimeSeconds
    ) {
        this.machineId = machineId;
        this.machineName = machineName;
        this.microStopCount = microStopCount;
        this.totalMicroStopSeconds = totalMicroStopSeconds;
        this.majorDowntimeCount = majorDowntimeCount;
        this.totalMajorDowntimeSeconds = totalMajorDowntimeSeconds;
        long totalEvents = microStopCount + majorDowntimeCount;
        this.microStopPercentage = totalEvents > 0 ? (double) microStopCount / totalEvents * 100.0 : 0.0;
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

    public long getMicroStopCount() {
        return microStopCount;
    }

    public void setMicroStopCount(long microStopCount) {
        this.microStopCount = microStopCount;
    }

    public long getTotalMicroStopSeconds() {
        return totalMicroStopSeconds;
    }

    public void setTotalMicroStopSeconds(long totalMicroStopSeconds) {
        this.totalMicroStopSeconds = totalMicroStopSeconds;
    }

    public long getMajorDowntimeCount() {
        return majorDowntimeCount;
    }

    public void setMajorDowntimeCount(long majorDowntimeCount) {
        this.majorDowntimeCount = majorDowntimeCount;
    }

    public long getTotalMajorDowntimeSeconds() {
        return totalMajorDowntimeSeconds;
    }

    public void setTotalMajorDowntimeSeconds(long totalMajorDowntimeSeconds) {
        this.totalMajorDowntimeSeconds = totalMajorDowntimeSeconds;
    }

    public double getMicroStopPercentage() {
        return microStopPercentage;
    }

    public void setMicroStopPercentage(double microStopPercentage) {
        this.microStopPercentage = microStopPercentage;
    }
}
