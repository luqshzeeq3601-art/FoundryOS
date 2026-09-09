package com.factoryos.modules.telemetry.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class TimeSeriesResponseDto {

    private UUID machineId;
    private String machineName;
    private String tagName;
    private String unit;
    private String bucketResolution;
    private Instant from;
    private Instant to;
    private int pointCount;
    private long queryExecutionMs;
    private List<TimeSeriesBucketDto> series;

    public TimeSeriesResponseDto() {
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

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getBucketResolution() {
        return bucketResolution;
    }

    public void setBucketResolution(String bucketResolution) {
        this.bucketResolution = bucketResolution;
    }

    public Instant getFrom() {
        return from;
    }

    public void setFrom(Instant from) {
        this.from = from;
    }

    public Instant getTo() {
        return to;
    }

    public void setTo(Instant to) {
        this.to = to;
    }

    public int getPointCount() {
        return pointCount;
    }

    public void setPointCount(int pointCount) {
        this.pointCount = pointCount;
    }

    public long getQueryExecutionMs() {
        return queryExecutionMs;
    }

    public void setQueryExecutionMs(long queryExecutionMs) {
        this.queryExecutionMs = queryExecutionMs;
    }

    public List<TimeSeriesBucketDto> getSeries() {
        return series;
    }

    public void setSeries(List<TimeSeriesBucketDto> series) {
        this.series = series;
    }
}
