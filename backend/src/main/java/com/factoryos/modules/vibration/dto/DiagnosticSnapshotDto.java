package com.factoryos.modules.vibration.dto;

import com.factoryos.modules.vibration.domain.FaultHarmonicType;
import com.factoryos.modules.vibration.domain.IsoSeverityZone;
import com.factoryos.modules.vibration.domain.MachineHealthStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class DiagnosticSnapshotDto {

    private UUID assessmentId;
    private UUID machineId;
    private String machineName;
    private String plantName;
    private String axis;
    private int healthScore;
    private MachineHealthStatus healthStatus;
    private IsoSeverityZone isoSeverityZone;
    private double rmsVelocityMmS;
    private Double spindleTemperatureC;
    private double crestFactor;
    private double kurtosis;
    private double runningSpeedRpm;
    private FaultHarmonicType dominantFault;
    private String suspectedSubsystem;
    private List<String> recommendedParts;
    private String prescriptiveAction;
    private List<SpectralPeakDto> dominantPeaks;
    private Instant capturedAt;

    public DiagnosticSnapshotDto() {
    }

    public UUID getAssessmentId() {
        return assessmentId;
    }

    public void setAssessmentId(UUID assessmentId) {
        this.assessmentId = assessmentId;
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

    public String getPlantName() {
        return plantName;
    }

    public void setPlantName(String plantName) {
        this.plantName = plantName;
    }

    public String getAxis() {
        return axis;
    }

    public void setAxis(String axis) {
        this.axis = axis;
    }

    public int getHealthScore() {
        return healthScore;
    }

    public void setHealthScore(int healthScore) {
        this.healthScore = healthScore;
    }

    public MachineHealthStatus getHealthStatus() {
        return healthStatus;
    }

    public void setHealthStatus(MachineHealthStatus healthStatus) {
        this.healthStatus = healthStatus;
    }

    public IsoSeverityZone getIsoSeverityZone() {
        return isoSeverityZone;
    }

    public void setIsoSeverityZone(IsoSeverityZone isoSeverityZone) {
        this.isoSeverityZone = isoSeverityZone;
    }

    public double getRmsVelocityMmS() {
        return rmsVelocityMmS;
    }

    public void setRmsVelocityMmS(double rmsVelocityMmS) {
        this.rmsVelocityMmS = rmsVelocityMmS;
    }

    public Double getSpindleTemperatureC() {
        return spindleTemperatureC;
    }

    public void setSpindleTemperatureC(Double spindleTemperatureC) {
        this.spindleTemperatureC = spindleTemperatureC;
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

    public double getRunningSpeedRpm() {
        return runningSpeedRpm;
    }

    public void setRunningSpeedRpm(double runningSpeedRpm) {
        this.runningSpeedRpm = runningSpeedRpm;
    }

    public FaultHarmonicType getDominantFault() {
        return dominantFault;
    }

    public void setDominantFault(FaultHarmonicType dominantFault) {
        this.dominantFault = dominantFault;
    }

    public String getSuspectedSubsystem() {
        return suspectedSubsystem;
    }

    public void setSuspectedSubsystem(String suspectedSubsystem) {
        this.suspectedSubsystem = suspectedSubsystem;
    }

    public List<String> getRecommendedParts() {
        return recommendedParts;
    }

    public void setRecommendedParts(List<String> recommendedParts) {
        this.recommendedParts = recommendedParts;
    }

    public String getPrescriptiveAction() {
        return prescriptiveAction;
    }

    public void setPrescriptiveAction(String prescriptiveAction) {
        this.prescriptiveAction = prescriptiveAction;
    }

    public List<SpectralPeakDto> getDominantPeaks() {
        return dominantPeaks;
    }

    public void setDominantPeaks(List<SpectralPeakDto> dominantPeaks) {
        this.dominantPeaks = dominantPeaks;
    }

    public Instant getCapturedAt() {
        return capturedAt;
    }

    public void setCapturedAt(Instant capturedAt) {
        this.capturedAt = capturedAt;
    }
}
