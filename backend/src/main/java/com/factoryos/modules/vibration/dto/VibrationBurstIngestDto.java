package com.factoryos.modules.vibration.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public class VibrationBurstIngestDto {

    @NotNull
    private UUID machineId;

    private String axis = "RADIAL_X";

    private double sampleRateHz = 2048.0;

    private Double runningSpeedRpm = 3000.0;

    private Double bearingTemperatureC = 45.0;

    @NotNull
    private List<Double> samples;

    public VibrationBurstIngestDto() {
    }

    public UUID getMachineId() {
        return machineId;
    }

    public void setMachineId(UUID machineId) {
        this.machineId = machineId;
    }

    public String getAxis() {
        return axis;
    }

    public void setAxis(String axis) {
        this.axis = axis;
    }

    public double getSampleRateHz() {
        return sampleRateHz;
    }

    public void setSampleRateHz(double sampleRateHz) {
        this.sampleRateHz = sampleRateHz;
    }

    public Double getRunningSpeedRpm() {
        return runningSpeedRpm;
    }

    public void setRunningSpeedRpm(Double runningSpeedRpm) {
        this.runningSpeedRpm = runningSpeedRpm;
    }

    public Double getBearingTemperatureC() {
        return bearingTemperatureC;
    }

    public void setBearingTemperatureC(Double bearingTemperatureC) {
        this.bearingTemperatureC = bearingTemperatureC;
    }

    public List<Double> getSamples() {
        return samples;
    }

    public void setSamples(List<Double> samples) {
        this.samples = samples;
    }
}
