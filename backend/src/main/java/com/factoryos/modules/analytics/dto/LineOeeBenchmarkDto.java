package com.factoryos.modules.analytics.dto;

import java.util.UUID;

public class LineOeeBenchmarkDto {

    private UUID lineId;
    private String lineCode;
    private String lineName;
    private UUID areaId;
    private String areaCode;
    private String areaName;

    private double oee;
    private double availability;
    private double performance;
    private double quality;

    private long totalGoodQuantity;
    private long totalScrapQuantity;
    private double scrapRate;

    private int totalMachines;
    private int runningMachines;
    private int idleMachines;
    private int downMachines;

    private boolean isBottleneck;
    private String status;

    public LineOeeBenchmarkDto() {
    }

    public LineOeeBenchmarkDto(
            UUID lineId,
            String lineCode,
            String lineName,
            UUID areaId,
            String areaCode,
            String areaName,
            double oee,
            double availability,
            double performance,
            double quality,
            long totalGoodQuantity,
            long totalScrapQuantity,
            double scrapRate,
            int totalMachines,
            int runningMachines,
            int idleMachines,
            int downMachines,
            boolean isBottleneck,
            String status
    ) {
        this.lineId = lineId;
        this.lineCode = lineCode;
        this.lineName = lineName;
        this.areaId = areaId;
        this.areaCode = areaCode;
        this.areaName = areaName;
        this.oee = oee;
        this.availability = availability;
        this.performance = performance;
        this.quality = quality;
        this.totalGoodQuantity = totalGoodQuantity;
        this.totalScrapQuantity = totalScrapQuantity;
        this.scrapRate = scrapRate;
        this.totalMachines = totalMachines;
        this.runningMachines = runningMachines;
        this.idleMachines = idleMachines;
        this.downMachines = downMachines;
        this.isBottleneck = isBottleneck;
        this.status = status;
    }

    public UUID getLineId() {
        return lineId;
    }

    public void setLineId(UUID lineId) {
        this.lineId = lineId;
    }

    public String getLineCode() {
        return lineCode;
    }

    public void setLineCode(String lineCode) {
        this.lineCode = lineCode;
    }

    public String getLineName() {
        return lineName;
    }

    public void setLineName(String lineName) {
        this.lineName = lineName;
    }

    public UUID getAreaId() {
        return areaId;
    }

    public void setAreaId(UUID areaId) {
        this.areaId = areaId;
    }

    public String getAreaCode() {
        return areaCode;
    }

    public void setAreaCode(String areaCode) {
        this.areaCode = areaCode;
    }

    public String getAreaName() {
        return areaName;
    }

    public void setAreaName(String areaName) {
        this.areaName = areaName;
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

    public boolean isBottleneck() {
        return isBottleneck;
    }

    public void setBottleneck(boolean bottleneck) {
        isBottleneck = bottleneck;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
