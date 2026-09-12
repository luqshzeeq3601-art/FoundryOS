package com.factoryos.modules.analytics.dto;

public class FleetSummaryDto {

    private int totalPlants;
    private int activeLines;
    private int totalMachines;
    private int runningMachines;
    private int idleMachines;
    private int downMachines;

    private double fleetAvgOee;
    private double fleetAvgAvailability;
    private double fleetAvgPerformance;
    private double fleetAvgQuality;

    private long totalGoodQuantity;
    private long totalScrapQuantity;
    private double fleetScrapRate;
    private long totalDowntimeMinutes;

    public FleetSummaryDto() {
    }

    public FleetSummaryDto(
            int totalPlants,
            int activeLines,
            int totalMachines,
            int runningMachines,
            int idleMachines,
            int downMachines,
            double fleetAvgOee,
            double fleetAvgAvailability,
            double fleetAvgPerformance,
            double fleetAvgQuality,
            long totalGoodQuantity,
            long totalScrapQuantity,
            double fleetScrapRate,
            long totalDowntimeMinutes
    ) {
        this.totalPlants = totalPlants;
        this.activeLines = activeLines;
        this.totalMachines = totalMachines;
        this.runningMachines = runningMachines;
        this.idleMachines = idleMachines;
        this.downMachines = downMachines;
        this.fleetAvgOee = fleetAvgOee;
        this.fleetAvgAvailability = fleetAvgAvailability;
        this.fleetAvgPerformance = fleetAvgPerformance;
        this.fleetAvgQuality = fleetAvgQuality;
        this.totalGoodQuantity = totalGoodQuantity;
        this.totalScrapQuantity = totalScrapQuantity;
        this.fleetScrapRate = fleetScrapRate;
        this.totalDowntimeMinutes = totalDowntimeMinutes;
    }

    public int getTotalPlants() {
        return totalPlants;
    }

    public void setTotalPlants(int totalPlants) {
        this.totalPlants = totalPlants;
    }

    public int getActiveLines() {
        return activeLines;
    }

    public void setActiveLines(int activeLines) {
        this.activeLines = activeLines;
    }

    public int getTotalMachines() {
        return totalMachines;
    }

    public void setTotalMachines(int totalMachines) {
        this.totalMachines = totalMachines;
    }

    public int getRunningMachines() {
        return runningMachines;
    }

    public void setRunningMachines(int runningMachines) {
        this.runningMachines = runningMachines;
    }

    public int getIdleMachines() {
        return idleMachines;
    }

    public void setIdleMachines(int idleMachines) {
        this.idleMachines = idleMachines;
    }

    public int getDownMachines() {
        return downMachines;
    }

    public void setDownMachines(int downMachines) {
        this.downMachines = downMachines;
    }

    public double getFleetAvgOee() {
        return fleetAvgOee;
    }

    public void setFleetAvgOee(double fleetAvgOee) {
        this.fleetAvgOee = fleetAvgOee;
    }

    public double getFleetAvgAvailability() {
        return fleetAvgAvailability;
    }

    public void setFleetAvgAvailability(double fleetAvgAvailability) {
        this.fleetAvgAvailability = fleetAvgAvailability;
    }

    public double getFleetAvgPerformance() {
        return fleetAvgPerformance;
    }

    public void setFleetAvgPerformance(double fleetAvgPerformance) {
        this.fleetAvgPerformance = fleetAvgPerformance;
    }

    public double getFleetAvgQuality() {
        return fleetAvgQuality;
    }

    public void setFleetAvgQuality(double fleetAvgQuality) {
        this.fleetAvgQuality = fleetAvgQuality;
    }

    public long getTotalGoodQuantity() {
        return totalGoodQuantity;
    }

    public void setTotalGoodQuantity(long totalGoodQuantity) {
        this.totalGoodQuantity = totalGoodQuantity;
    }

    public long getTotalScrapQuantity() {
        return totalScrapQuantity;
    }

    public void setTotalScrapQuantity(long totalScrapQuantity) {
        this.totalScrapQuantity = totalScrapQuantity;
    }

    public double getFleetScrapRate() {
        return fleetScrapRate;
    }

    public void setFleetScrapRate(double fleetScrapRate) {
        this.fleetScrapRate = fleetScrapRate;
    }

    public long getTotalDowntimeMinutes() {
        return totalDowntimeMinutes;
    }

    public void setTotalDowntimeMinutes(long totalDowntimeMinutes) {
        this.totalDowntimeMinutes = totalDowntimeMinutes;
    }
}
