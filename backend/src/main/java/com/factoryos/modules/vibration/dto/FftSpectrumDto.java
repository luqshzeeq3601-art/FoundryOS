package com.factoryos.modules.vibration.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FftSpectrumDto {

    private UUID machineId;
    private String machineName;
    private String axis;
    private double sampleRateHz;
    private int sampleCount;
    private Double runningSpeedRpm;
    private Double fundamentalFrequencyHz;
    private double[] frequencies;
    private double[] amplitudes;
    private List<SpectralPeakDto> peaks = new ArrayList<>();
    private double rmsVelocityMmS;
    private double peakAccelerationG;
    private double crestFactor;
    private double kurtosis;
    private Double bearingTemperatureC;
    private Instant capturedAt;

    public FftSpectrumDto() {
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

    public int getSampleCount() {
        return sampleCount;
    }

    public void setSampleCount(int sampleCount) {
        this.sampleCount = sampleCount;
    }

    public Double getRunningSpeedRpm() {
        return runningSpeedRpm;
    }

    public void setRunningSpeedRpm(Double runningSpeedRpm) {
        this.runningSpeedRpm = runningSpeedRpm;
    }

    public Double getFundamentalFrequencyHz() {
        return fundamentalFrequencyHz;
    }

    public void setFundamentalFrequencyHz(Double fundamentalFrequencyHz) {
        this.fundamentalFrequencyHz = fundamentalFrequencyHz;
    }

    public double[] getFrequencies() {
        return frequencies;
    }

    public void setFrequencies(double[] frequencies) {
        this.frequencies = frequencies;
    }

    public double[] getAmplitudes() {
        return amplitudes;
    }

    public void setAmplitudes(double[] amplitudes) {
        this.amplitudes = amplitudes;
    }

    public List<SpectralPeakDto> getPeaks() {
        return peaks;
    }

    public void setPeaks(List<SpectralPeakDto> peaks) {
        this.peaks = peaks;
    }

    public double getRmsVelocityMmS() {
        return rmsVelocityMmS;
    }

    public void setRmsVelocityMmS(double rmsVelocityMmS) {
        this.rmsVelocityMmS = rmsVelocityMmS;
    }

    public double getPeakAccelerationG() {
        return peakAccelerationG;
    }

    public void setPeakAccelerationG(double peakAccelerationG) {
        this.peakAccelerationG = peakAccelerationG;
    }

    public double getCrestFactor() {
        return crestFactor;
    }

    public void setCrestFactor(double crestFactor) {
        this.crestFactor = crestFactor;
    }

    public double getKurtosis() {
        return kurtosis;
    }

    public void setKurtosis(double kurtosis) {
        this.kurtosis = kurtosis;
    }

    public Double getBearingTemperatureC() {
        return bearingTemperatureC;
    }

    public void setBearingTemperatureC(Double bearingTemperatureC) {
        this.bearingTemperatureC = bearingTemperatureC;
    }

    public Instant getCapturedAt() {
        return capturedAt;
    }

    public void setCapturedAt(Instant capturedAt) {
        this.capturedAt = capturedAt;
    }
}
