package com.factoryos.modules.analytics.application;

import com.factoryos.common.dto.ApiResponse;
import com.factoryos.modules.analytics.dto.EnterpriseOeeMatrixDto;
import com.factoryos.modules.analytics.dto.ScheduledReportResponseDto;
import com.factoryos.modules.auth.domain.User;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@RestController
@RequestMapping("/api/v2/analytics/enterprise")
public class EnterpriseAnalyticsController {

    private static final DateTimeFormatter FILENAME_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
            .withZone(ZoneId.of("UTC"));

    private final EnterpriseAnalyticsService analyticsService;
    private final EnterpriseExportService exportService;
    private final ScheduledEnterpriseReportService scheduledReportService;

    public EnterpriseAnalyticsController(
            EnterpriseAnalyticsService analyticsService,
            EnterpriseExportService exportService,
            ScheduledEnterpriseReportService scheduledReportService
    ) {
        this.analyticsService = analyticsService;
        this.exportService = exportService;
        this.scheduledReportService = scheduledReportService;
    }

    @GetMapping("/oee-matrix")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'VIEWER')")
    public ResponseEntity<ApiResponse<EnterpriseOeeMatrixDto>> getEnterpriseOeeMatrix(
            @RequestParam(required = false, defaultValue = "24H") String interval,
            @RequestParam(required = false) UUID enterpriseId,
            @RequestParam(required = false) String status
    ) {
        EnterpriseOeeMatrixDto matrix = analyticsService.getEnterpriseOeeMatrix(interval, enterpriseId, status);
        return ResponseEntity.ok(ApiResponse.ok(matrix));
    }

    @GetMapping(value = "/export/csv", produces = "text/csv")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'VIEWER')")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(required = false, defaultValue = "24H") String interval,
            @RequestParam(required = false) UUID enterpriseId,
            @RequestParam(required = false) String status
    ) {
        EnterpriseOeeMatrixDto matrix = analyticsService.getEnterpriseOeeMatrix(interval, enterpriseId, status);
        byte[] csvData = exportService.generateCsvReport(matrix);

        String filename = String.format("factoryos_fleet_oee_%s_%s.csv",
                interval.toLowerCase(), FILENAME_DATE_FORMATTER.format(Instant.now()));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvData);
    }

    @GetMapping(value = "/export/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'VIEWER')")
    public ResponseEntity<byte[]> exportPdf(
            @RequestParam(required = false, defaultValue = "24H") String interval,
            @RequestParam(required = false) UUID enterpriseId,
            @RequestParam(required = false) String status
    ) {
        EnterpriseOeeMatrixDto matrix = analyticsService.getEnterpriseOeeMatrix(interval, enterpriseId, status);
        byte[] pdfData = exportService.generatePdfReport(matrix);

        String filename = String.format("factoryos_fleet_summary_%s_%s.pdf",
                interval.toLowerCase(), FILENAME_DATE_FORMATTER.format(Instant.now()));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfData);
    }

    @PostMapping("/reports/scheduled")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER')")
    public ResponseEntity<ApiResponse<ScheduledReportResponseDto>> triggerScheduledReport(
            @RequestParam(required = false, defaultValue = "PDF") String format,
            @RequestParam(required = false, defaultValue = "24H") String interval,
            @AuthenticationPrincipal User actor
    ) {
        ScheduledReportResponseDto result = scheduledReportService.generateAndAuditReport(format, interval, actor);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
