package com.factoryos.modules.telemetry.domain;

import com.factoryos.modules.machine.domain.Machine;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "machine_telemetry_points")
public class MachineTelemetryPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id", nullable = false)
    private Machine machine;

    @Column(name = "tag_name", nullable = false, length = 100)
    private String tagName;

    @Column(name = "metric_value", nullable = false)
    private double metricValue;

    @Column(name = "metric_unit", length = 32)
    private String metricUnit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TelemetryQuality quality = TelemetryQuality.GOOD;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(name = "ingested_at", nullable = false, updatable = false)
    private Instant ingestedAt = Instant.now();

    public MachineTelemetryPoint() {
    }

    public MachineTelemetryPoint(Machine machine, String tagName, double metricValue, String metricUnit, TelemetryQuality quality, Instant timestamp) {
        this.machine = machine;
        this.tagName = tagName;
        this.metricValue = metricValue;
        this.metricUnit = metricUnit;
        this.quality = quality != null ? quality : TelemetryQuality.GOOD;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.ingestedAt = Instant.now();
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

    public double getMetricValue() {
        return metricValue;
    }

    public void setMetricValue(double metricValue) {
        this.metricValue = metricValue;
    }

    public String getMetricUnit() {
        return metricUnit;
    }

    public void setMetricUnit(String metricUnit) {
        this.metricUnit = metricUnit;
    }

    public TelemetryQuality getQuality() {
        return quality;
    }

    public void setQuality(TelemetryQuality quality) {
        this.quality = quality;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Instant getIngestedAt() {
        return ingestedAt;
    }

    public void setIngestedAt(Instant ingestedAt) {
        this.ingestedAt = ingestedAt;
    }
}
