package com.factoryos.modules.analytics.dto;

import java.util.List;
import java.util.UUID;

public class PlantOeeBenchmarkDto {

    private UUID plantId;
    private String plantCode;
    private String plantName;
    private String timezone;
    private String address;
    private String status;

    private int rank;
    private double oee;
    private double availability;
    private double performance;
    private double quality;
    private double oeeDeltaVsFleetAvg;
    private String benchmarkTier; // "WORLD_CLASS", "TARGET", "UNDERPERFORMING"

    private long totalGoodQuantity;
    private long totalScrapQuantity;
    private double scrapRate;

    private int totalMachines;
    private int runningMachines;
    private int idleMachines;
    private int downMachines;

    private long totalDowntimeMinutes;
    private long downtimeEventsCount;
    private int activeOrdersCount;
    private int completedOrdersCount;

    private List<LineOeeBenchmarkDto> lineMetrics;

    public PlantOeeBenchmarkDto() {
    }

    public UUID getPlantId() {
        return plantId;
    }

    public void setPlantId(UUID plantId) {
        this.plantId = plantId;
    }

    public String getPlantCode() {
        return plantCode;
    }

    public void setPlantCode(String plantCode) {
        this.plantCode = plantCode;
    }

    public String getPlantName() {
        return plantName;
    }

    public void setPlantName(String plantName) {
        this.plantName = plantName;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public double getOee() {
        return oee;
    }

    public void setOee(double oee) {
        this.oee = oee;
    }

    public double getAvailability() {
        return availability;
    }

    public void setAvailability(double availability) {
        this.availability = availability;
    }

    public double getPerformance() {
        return performance;
    }

    public void setPerformance(double performance) {
        this.performance = performance;
    }

    public double getQuality() {
        return quality;
    }

    public void setQuality(double quality) {
        this.quality = quality;
    }

    public double getOeeDeltaVsFleetAvg() {
        return oeeDeltaVsFleetAvg;
    }

    public void setOeeDeltaVsFleetAvg(double oeeDeltaVsFleetAvg) {
        this.oeeDeltaVsFleetAvg = oeeDeltaVsFleetAvg;
    }

    public String getBenchmarkTier() {
        return benchmarkTier;
    }

    public void setBenchmarkTier(String benchmarkTier) {
        this.benchmarkTier = benchmarkTier;
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

    public double getScrapRate() {
        return scrapRate;
    }

    public void setScrapRate(double scrapRate) {
        this.scrapRate = scrapRate;
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

    public long getTotalDowntimeMinutes() {
        return totalDowntimeMinutes;
    }

    public void setTotalDowntimeMinutes(long totalDowntimeMinutes) {
        this.totalDowntimeMinutes = totalDowntimeMinutes;
    }

    public long getDowntimeEventsCount() {
        return downtimeEventsCount;
    }

    public void setDowntimeEventsCount(long downtimeEventsCount) {
        this.downtimeEventsCount = downtimeEventsCount;
    }

    public int getActiveOrdersCount() {
        return activeOrdersCount;
    }

    public void setActiveOrdersCount(int activeOrdersCount) {
        this.activeOrdersCount = activeOrdersCount;
    }

    public int getCompletedOrdersCount() {
        return completedOrdersCount;
    }

    public void setCompletedOrdersCount(int completedOrdersCount) {
        this.completedOrdersCount = completedOrdersCount;
    }

    public List<LineOeeBenchmarkDto> getLineMetrics() {
        return lineMetrics;
    }

    public void setLineMetrics(List<LineOeeBenchmarkDto> lineMetrics) {
        this.lineMetrics = lineMetrics;
    }
}
