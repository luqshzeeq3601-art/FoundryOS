package com.factoryos.modules.analytics.dto;

import java.time.Instant;
import java.util.UUID;

public class ScheduledReportResponseDto {

    private UUID reportId;
    private String reportType; // "ENTERPRISE_OEE_EXECUTIVE_SUMMARY"
    private String format; // "CSV", "PDF"
    private String status; // "GENERATED", "DISPATCHED"
    private int plantCount;
    private double fleetOee;
    private long fileSizeBytes;
    private Instant generatedAt;
    private String message;

    public ScheduledReportResponseDto() {
    }

    public ScheduledReportResponseDto(
            UUID reportId,
            String reportType,
            String format,
            String status,
            int plantCount,
            double fleetOee,
            long fileSizeBytes,
            Instant generatedAt,
            String message
    ) {
        this.reportId = reportId;
        this.reportType = reportType;
        this.format = format;
        this.status = status;
        this.plantCount = plantCount;
        this.fleetOee = fleetOee;
        this.fileSizeBytes = fileSizeBytes;
        this.generatedAt = generatedAt;
        this.message = message;
    }

    public UUID getReportId() {
        return reportId;
    }

    public void setReportId(UUID reportId) {
        this.reportId = reportId;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getPlantCount() {
        return plantCount;
    }

    public void setPlantCount(int plantCount) {
        this.plantCount = plantCount;
    }

    public double getFleetOee() {
        return fleetOee;
    }

    public void setFleetOee(double fleetOee) {
        this.fleetOee = fleetOee;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Instant generatedAt) {
        this.generatedAt = generatedAt;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
