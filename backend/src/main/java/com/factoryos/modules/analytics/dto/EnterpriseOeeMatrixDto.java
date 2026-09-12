package com.factoryos.modules.analytics.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class EnterpriseOeeMatrixDto {

    private UUID enterpriseId;
    private String enterpriseCode;
    private String enterpriseName;
    private String interval;
    private Instant calculatedAt;

    private FleetSummaryDto fleetSummary;
    private List<PlantOeeBenchmarkDto> plantMetrics;

    public EnterpriseOeeMatrixDto() {
        this.calculatedAt = Instant.now();
    }

    public EnterpriseOeeMatrixDto(
            UUID enterpriseId,
            String enterpriseCode,
            String enterpriseName,
            String interval,
            Instant calculatedAt,
            FleetSummaryDto fleetSummary,
            List<PlantOeeBenchmarkDto> plantMetrics
    ) {
        this.enterpriseId = enterpriseId;
        this.enterpriseCode = enterpriseCode;
        this.enterpriseName = enterpriseName;
        this.interval = interval;
        this.calculatedAt = calculatedAt != null ? calculatedAt : Instant.now();
        this.fleetSummary = fleetSummary;
        this.plantMetrics = plantMetrics;
    }

    public UUID getEnterpriseId() {
        return enterpriseId;
    }

    public void setEnterpriseId(UUID enterpriseId) {
        this.enterpriseId = enterpriseId;
    }

    public String getEnterpriseCode() {
        return enterpriseCode;
    }

    public void setEnterpriseCode(String enterpriseCode) {
        this.enterpriseCode = enterpriseCode;
    }

    public String getEnterpriseName() {
        return enterpriseName;
    }

    public void setEnterpriseName(String enterpriseName) {
        this.enterpriseName = enterpriseName;
    }

    public String getInterval() {
        return interval;
    }

    public void setInterval(String interval) {
        this.interval = interval;
    }

    public Instant getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(Instant calculatedAt) {
        this.calculatedAt = calculatedAt;
    }

    public FleetSummaryDto getFleetSummary() {
        return fleetSummary;
    }

    public void setFleetSummary(FleetSummaryDto fleetSummary) {
        this.fleetSummary = fleetSummary;
    }

    public List<PlantOeeBenchmarkDto> getPlantMetrics() {
        return plantMetrics;
    }

    public void setPlantMetrics(List<PlantOeeBenchmarkDto> plantMetrics) {
        this.plantMetrics = plantMetrics;
    }
}
