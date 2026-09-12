package com.factoryos.modules.vibration.dto;

import com.factoryos.modules.vibration.domain.FaultHarmonicType;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class SimulateBurstRequestDto {

    @NotNull
    private UUID machineId;

    private FaultHarmonicType faultType = FaultHarmonicType.NORMAL;

    private double runningSpeedRpm = 3000.0;

    private double sampleRateHz = 2048.0;

    private int sampleCount = 1024;

    private double noiseLevel = 0.05;

    private Double bearingTemperatureC;

    public SimulateBurstRequestDto() {
    }

    public UUID getMachineId() {
        return machineId;
    }

    public void setMachineId(UUID machineId) {
        this.machineId = machineId;
    }

    public FaultHarmonicType getFaultType() {
        return faultType;
    }

    public void setFaultType(FaultHarmonicType faultType) {
        this.faultType = faultType;
    }

    public double getRunningSpeedRpm() {
        return runningSpeedRpm;
    }

    public void setRunningSpeedRpm(double runningSpeedRpm) {
        this.runningSpeedRpm = runningSpeedRpm;
    }

    public double getSampleRateHz() {
        return sampleRateHz;
    }

    public void setSampleRateHz(double sampleRateHz) {
        this.sampleRateHz = sampleRateHz;
    }

    public int getSampleCount() {
        return sampleCount;
    }

    public void setSampleCount(int sampleCount) {
        this.sampleCount = sampleCount;
    }

    public double getNoiseLevel() {
        return noiseLevel;
    }

    public void setNoiseLevel(double noiseLevel) {
        this.noiseLevel = noiseLevel;
    }

    public Double getBearingTemperatureC() {
        return bearingTemperatureC;
    }

    public void setBearingTemperatureC(Double bearingTemperatureC) {
        this.bearingTemperatureC = bearingTemperatureC;
    }
}
