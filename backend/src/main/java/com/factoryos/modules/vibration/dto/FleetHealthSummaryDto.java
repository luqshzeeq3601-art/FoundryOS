package com.factoryos.modules.vibration.dto;

import java.util.ArrayList;
import java.util.List;

public class FleetHealthSummaryDto {

    private int totalMachinesAssessed;
    private double averageFleetHealthScore;
    private int zoneACount;
    private int zoneBCount;
    private int zoneCCount;
    private int zoneDCount;
    private int criticalCount;
    private int warningCount;
    private int fairCount;
    private int healthyCount;
    private List<MachineHealthAssessmentDto> assessments = new ArrayList<>();

    public FleetHealthSummaryDto() {
    }

    public int getTotalMachinesAssessed() {
        return totalMachinesAssessed;
    }

    public void setTotalMachinesAssessed(int totalMachinesAssessed) {
        this.totalMachinesAssessed = totalMachinesAssessed;
    }

    public double getAverageFleetHealthScore() {
        return averageFleetHealthScore;
    }

    public void setAverageFleetHealthScore(double averageFleetHealthScore) {
        this.averageFleetHealthScore = averageFleetHealthScore;
    }

    public int getZoneACount() {
        return zoneACount;
    }

    public void setZoneACount(int zoneACount) {
        this.zoneACount = zoneACount;
    }

    public int getZoneBCount() {
        return zoneBCount;
    }

    public void setZoneBCount(int zoneBCount) {
        this.zoneBCount = zoneBCount;
    }

    public int getZoneCCount() {
        return zoneCCount;
    }

    public void setZoneCCount(int zoneCCount) {
        this.zoneCCount = zoneCCount;
    }

    public int getZoneDCount() {
        return zoneDCount;
    }

    public void setZoneDCount(int zoneDCount) {
        this.zoneDCount = zoneDCount;
    }

    public int getCriticalCount() {
        return criticalCount;
    }

    public void setCriticalCount(int criticalCount) {
        this.criticalCount = criticalCount;
    }

    public int getWarningCount() {
        return warningCount;
    }

    public void setWarningCount(int warningCount) {
        this.warningCount = warningCount;
    }

    public int getFairCount() {
        return fairCount;
    }

    public void setFairCount(int fairCount) {
        this.fairCount = fairCount;
    }

    public int getHealthyCount() {
        return healthyCount;
    }

    public void setHealthyCount(int healthyCount) {
        this.healthyCount = healthyCount;
    }

    public List<MachineHealthAssessmentDto> getAssessments() {
        return assessments;
    }

    public void setAssessments(List<MachineHealthAssessmentDto> assessments) {
        this.assessments = assessments;
    }
}
