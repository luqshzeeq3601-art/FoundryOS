package com.factoryos.modules.telemetry.dto;

import com.factoryos.modules.telemetry.domain.TelemetryQuality;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public class TelemetryPointDto {

    @NotBlank(message = "Tag name is required")
    private String tagName;

    @NotNull(message = "Metric value is required")
    private Double value;

    private String unit;
    private TelemetryQuality quality = TelemetryQuality.GOOD;
    private Instant timestamp;

    public TelemetryPointDto() {
    }

    public TelemetryPointDto(String tagName, Double value, String unit, TelemetryQuality quality, Instant timestamp) {
        this.tagName = tagName;
        this.value = value;
        this.unit = unit;
        this.quality = quality;
        this.timestamp = timestamp;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
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
}
