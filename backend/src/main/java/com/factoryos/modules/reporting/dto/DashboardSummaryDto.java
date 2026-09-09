package com.factoryos.modules.reporting.dto;

import java.util.Map;

public class DashboardSummaryDto {
    private long totalMachines;
    private long runningMachines;
    private long idleMachines;
    private long downMachines;

    private long totalProductionOrders;
    private long activeProductionOrders;
    private long completedProductionOrders;
    private long totalGoodQuantity;
    private long totalScrapQuantity;
    private double scrapRate;

    private long openDowntimeEvents;
    private long openWorkOrders;
    private long inProgressWorkOrders;
    private long criticalWorkOrders;

    private double plantAvailability;
    private double plantPerformance;
    private double plantQuality;
    private double plantOee;

    private Map<String, Long> downtimeReasonBreakdown;

    public DashboardSummaryDto() {
    }

    public long getTotalMachines() {
        return totalMachines;
    }

    public void setTotalMachines(long totalMachines) {
        this.totalMachines = totalMachines;
    }

    public long getRunningMachines() {
        return runningMachines;
    }

    public void setRunningMachines(long runningMachines) {
        this.runningMachines = runningMachines;
    }

    public long getIdleMachines() {
        return idleMachines;
    }

    public void setIdleMachines(long idleMachines) {
        this.idleMachines = idleMachines;
    }

    public long getDownMachines() {
        return downMachines;
    }

    public void setDownMachines(long downMachines) {
        this.downMachines = downMachines;
    }

    public long getTotalProductionOrders() {
        return totalProductionOrders;
    }

    public void setTotalProductionOrders(long totalProductionOrders) {
        this.totalProductionOrders = totalProductionOrders;
    }

    public long getActiveProductionOrders() {
        return activeProductionOrders;
    }

    public void setActiveProductionOrders(long activeProductionOrders) {
        this.activeProductionOrders = activeProductionOrders;
    }

    public long getCompletedProductionOrders() {
        return completedProductionOrders;
    }

    public void setCompletedProductionOrders(long completedProductionOrders) {
        this.completedProductionOrders = completedProductionOrders;
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

    public long getOpenDowntimeEvents() {
        return openDowntimeEvents;
    }

    public void setOpenDowntimeEvents(long openDowntimeEvents) {
        this.openDowntimeEvents = openDowntimeEvents;
    }

    public long getOpenWorkOrders() {
        return openWorkOrders;
    }

    public void setOpenWorkOrders(long openWorkOrders) {
        this.openWorkOrders = openWorkOrders;
    }

    public long getInProgressWorkOrders() {
        return inProgressWorkOrders;
    }

    public void setInProgressWorkOrders(long inProgressWorkOrders) {
        this.inProgressWorkOrders = inProgressWorkOrders;
    }

    public long getCriticalWorkOrders() {
        return criticalWorkOrders;
    }

    public void setCriticalWorkOrders(long criticalWorkOrders) {
        this.criticalWorkOrders = criticalWorkOrders;
    }

    public double getPlantAvailability() {
        return plantAvailability;
    }

    public void setPlantAvailability(double plantAvailability) {
        this.plantAvailability = plantAvailability;
    }

    public double getPlantPerformance() {
        return plantPerformance;
    }

    public void setPlantPerformance(double plantPerformance) {
        this.plantPerformance = plantPerformance;
    }

    public double getPlantQuality() {
        return plantQuality;
    }

    public void setPlantQuality(double plantQuality) {
        this.plantQuality = plantQuality;
    }

    public double getPlantOee() {
        return plantOee;
    }

    public void setPlantOee(double plantOee) {
        this.plantOee = plantOee;
    }

    public Map<String, Long> getDowntimeReasonBreakdown() {
        return downtimeReasonBreakdown;
    }

    public void setDowntimeReasonBreakdown(Map<String, Long> downtimeReasonBreakdown) {
        this.downtimeReasonBreakdown = downtimeReasonBreakdown;
    }
}
