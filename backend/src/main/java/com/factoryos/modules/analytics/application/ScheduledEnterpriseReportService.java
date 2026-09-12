package com.factoryos.modules.analytics.application;

import com.factoryos.modules.analytics.dto.EnterpriseOeeMatrixDto;
import com.factoryos.modules.analytics.dto.ScheduledReportResponseDto;
import com.factoryos.modules.audit.application.AuditRecordingService;
import com.factoryos.modules.auth.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class ScheduledEnterpriseReportService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledEnterpriseReportService.class);

    private final EnterpriseAnalyticsService analyticsService;
    private final EnterpriseExportService exportService;
    private final AuditRecordingService auditRecordingService;

    public ScheduledEnterpriseReportService(
            EnterpriseAnalyticsService analyticsService,
            EnterpriseExportService exportService,
            AuditRecordingService auditRecordingService
    ) {
        this.analyticsService = analyticsService;
        this.exportService = exportService;
        this.auditRecordingService = auditRecordingService;
    }

    /**
     * Nightly scheduled executive report generation at 00:00 UTC.
     */
    @Scheduled(cron = "${factoryos.reports.enterprise.cron:0 0 0 * * ?}")
    public void runScheduledNightlyEnterpriseReport() {
        log.info("Executing scheduled nightly enterprise fleet OEE benchmark report generation...");
        try {
            generateAndAuditReport("PDF", "24H", null);
            generateAndAuditReport("CSV", "24H", null);
            log.info("Scheduled nightly enterprise reports generated successfully.");
        } catch (Exception e) {
            log.error("Failed to run scheduled nightly enterprise report", e);
        }
    }

    public ScheduledReportResponseDto generateAndAuditReport(String formatStr, String intervalStr, User actor) {
        String format = (formatStr != null && !formatStr.isBlank()) ? formatStr.trim().toUpperCase() : "PDF";
        String interval = (intervalStr != null && !intervalStr.isBlank()) ? intervalStr.trim().toUpperCase() : "24H";

        EnterpriseOeeMatrixDto matrix = analyticsService.getEnterpriseOeeMatrix(interval, null, null);

        byte[] reportBytes;
        if ("CSV".equalsIgnoreCase(format)) {
            reportBytes = exportService.generateCsvReport(matrix);
        } else {
            reportBytes = exportService.generatePdfReport(matrix);
        }

        UUID reportId = UUID.randomUUID();
        UUID actorId = actor != null ? actor.getId() : null;

        auditRecordingService.record(
                actorId,
                "ENTERPRISE_REPORT_GENERATED",
                "EnterpriseReport",
                reportId,
                null,
                Map.of(
                        "format", format,
                        "interval", interval,
                        "plantCount", String.valueOf(matrix.getPlantMetrics().size()),
                        "fleetOee", String.valueOf(matrix.getFleetSummary().getFleetAvgOee()),
                        "fileSizeBytes", String.valueOf(reportBytes.length)
                )
        );

        return new ScheduledReportResponseDto(
                reportId,
                "ENTERPRISE_OEE_EXECUTIVE_SUMMARY",
                format,
                "GENERATED",
                matrix.getPlantMetrics().size(),
                matrix.getFleetSummary().getFleetAvgOee(),
                reportBytes.length,
                Instant.now(),
                String.format("Executive %s report successfully generated for interval %s.", format, interval)
        );
    }
}
