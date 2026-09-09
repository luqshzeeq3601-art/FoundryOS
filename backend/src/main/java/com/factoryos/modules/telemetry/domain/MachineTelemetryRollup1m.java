package com.factoryos.modules.telemetry.domain;

import com.factoryos.modules.machine.domain.Machine;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "machine_telemetry_rollups_1m")
public class MachineTelemetryRollup1m {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id", nullable = false)
    private Machine machine;

    @Column(name = "tag_name", nullable = false, length = 100)
    private String tagName;

    @Column(name = "bucket_start", nullable = false)
    private Instant bucketStart;

    @Column(name = "min_value", nullable = false)
    private double minValue;

    @Column(name = "max_value", nullable = false)
    private double maxValue;

    @Column(name = "avg_value", nullable = false)
    private double avgValue;

    @Column(name = "sample_count", nullable = false)
    private int sampleCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TelemetryQuality quality = TelemetryQuality.GOOD;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public MachineTelemetryRollup1m() {
    }

    public MachineTelemetryRollup1m(Machine machine, String tagName, Instant bucketStart, double minValue, double maxValue, double avgValue, int sampleCount, TelemetryQuality quality) {
        this.machine = machine;
        this.tagName = tagName;
        this.bucketStart = bucketStart;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.avgValue = avgValue;
        this.sampleCount = sampleCount;
        this.quality = quality != null ? quality : TelemetryQuality.GOOD;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Machine getMachine() {
        return machine;
    }

    public void setMachine(Machine machine) {
        this.machine = machine;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public Instant getBucketStart() {
        return bucketStart;
    }

    public void setBucketStart(Instant bucketStart) {
        this.bucketStart = bucketStart;
    }

    public double getMinValue() {
        return minValue;
    }

    public void setMinValue(double minValue) {
        this.minValue = minValue;
    }

    public double getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(double maxValue) {
        this.maxValue = maxValue;
    }

    public double getAvgValue() {
        return avgValue;
    }

    public void setAvgValue(double avgValue) {
        this.avgValue = avgValue;
    }

    public int getSampleCount() {
        return sampleCount;
    }

    public void setSampleCount(int sampleCount) {
        this.sampleCount = sampleCount;
    }

    public TelemetryQuality getQuality() {
        return quality;
    }

    public void setQuality(TelemetryQuality quality) {
        this.quality = quality;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
