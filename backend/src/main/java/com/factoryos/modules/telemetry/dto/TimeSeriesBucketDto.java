package com.factoryos.modules.telemetry.dto;

import java.time.Instant;

public class TimeSeriesBucketDto {

    private Instant bucket;
    private double avg;
    private double min;
    private double max;
    private int count;
    private String quality;

    public TimeSeriesBucketDto() {
    }

    public TimeSeriesBucketDto(Instant bucket, double avg, double min, double max, int count, String quality) {
        this.bucket = bucket;
        this.avg = avg;
        this.min = min;
        this.max = max;
        this.count = count;
        this.quality = quality;
    }

    public Instant getBucket() {
        return bucket;
    }

    public void setBucket(Instant bucket) {
        this.bucket = bucket;
    }

    public double getAvg() {
        return avg;
    }

    public void setAvg(double avg) {
        this.avg = avg;
    }

    public double getMin() {
        return min;
    }

    public void setMin(double min) {
        this.min = min;
    }

    public double getMax() {
        return max;
    }

    public void setMax(double max) {
        this.max = max;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getQuality() {
        return quality;
    }

    public void setQuality(String quality) {
        this.quality = quality;
    }
}
