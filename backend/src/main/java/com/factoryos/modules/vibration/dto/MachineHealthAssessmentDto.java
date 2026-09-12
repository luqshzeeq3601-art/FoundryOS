package com.factoryos.modules.vibration.dto;

import com.factoryos.modules.vibration.domain.IsoSeverityZone;
import com.factoryos.modules.vibration.domain.MachineHealthStatus;
import com.factoryos.modules.vibration.domain.MachineVibrationClass;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MachineHealthAssessmentDto {

    private UUID id;
    private UUID machineId;
    private String machineName;
    private String serialNumber;
    private String location;
    private UUID plantId;
    private String plantName;
    private int healthScore;
    private MachineHealthStatus healthStatus;
    private String healthStatusDescription;
    private IsoSeverityZone isoSeverityZone;
    private String isoZoneTitle;
    private MachineVibrationClass vibrationClass;
    private double rmsVelocityMmS;
    private Double spindleTemperatureC;
    private String dominantFaultType;
    private String diagnosisSummary;
    private String recommendedAction;
    private List<SpectralPeakDto> dominantPeaks = new ArrayList<>();
    private com.factoryos.modules.maintenance.dto.WorkOrderDto activeWorkOrder;
    private Instant assessedAt;

    public MachineHealthAssessmentDto() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
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

    public String getHealthStatusDescription() {
        return healthStatusDescription;
    }

    public void setHealthStatusDescription(String healthStatusDescription) {
        this.healthStatusDescription = healthStatusDescription;
    }

    public IsoSeverityZone getIsoSeverityZone() {
        return isoSeverityZone;
    }

    public void setIsoSeverityZone(IsoSeverityZone isoSeverityZone) {
        this.isoSeverityZone = isoSeverityZone;
    }

    public String getIsoZoneTitle() {
        return isoZoneTitle;
    }

    public void setIsoZoneTitle(String isoZoneTitle) {
        this.isoZoneTitle = isoZoneTitle;
    }

    public MachineVibrationClass getVibrationClass() {
        return vibrationClass;
    }

    public void setVibrationClass(MachineVibrationClass vibrationClass) {
        this.vibrationClass = vibrationClass;
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

    public String getDominantFaultType() {
        return dominantFaultType;
    }

    public void setDominantFaultType(String dominantFaultType) {
        this.dominantFaultType = dominantFaultType;
    }

    public String getDiagnosisSummary() {
        return diagnosisSummary;
    }

    public void setDiagnosisSummary(String diagnosisSummary) {
        this.diagnosisSummary = diagnosisSummary;
    }

    public String getRecommendedAction() {
        return recommendedAction;
    }

    public void setRecommendedAction(String recommendedAction) {
        this.recommendedAction = recommendedAction;
    }

    public List<SpectralPeakDto> getDominantPeaks() {
        return dominantPeaks;
    }

    public void setDominantPeaks(List<SpectralPeakDto> dominantPeaks) {
        this.dominantPeaks = dominantPeaks;
    }

    public com.factoryos.modules.maintenance.dto.WorkOrderDto getActiveWorkOrder() {
        return activeWorkOrder;
    }

    public void setActiveWorkOrder(com.factoryos.modules.maintenance.dto.WorkOrderDto activeWorkOrder) {
        this.activeWorkOrder = activeWorkOrder;
    }

    public Instant getAssessedAt() {
        return assessedAt;
    }

    public void setAssessedAt(Instant assessedAt) {
        this.assessedAt = assessedAt;
    }
}
