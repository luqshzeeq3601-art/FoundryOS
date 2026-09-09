package com.factoryos.modules.telemetry.application;

import com.factoryos.common.dto.ApiResponse;
import com.factoryos.modules.auth.domain.User;
import com.factoryos.modules.telemetry.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v2/telemetry")
public class TelemetryController {

    private final TelemetryIngestionService telemetryService;
    private final TelemetryDownsamplingService downsamplingService;

    public TelemetryController(
            TelemetryIngestionService telemetryService,
            TelemetryDownsamplingService downsamplingService
    ) {
        this.telemetryService = telemetryService;
        this.downsamplingService = downsamplingService;
    }

    @PostMapping("/machines/{machineId}/tags")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER')")
    public ResponseEntity<ApiResponse<TagMappingDto>> createTagMapping(
            @PathVariable UUID machineId,
            @Valid @RequestBody CreateTagMappingRequest request,
            @AuthenticationPrincipal User actor
    ) {
        TagMappingDto dto = telemetryService.createTagMapping(machineId, request, actor);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(dto, "Tag mapping registered successfully"));
    }

    @GetMapping("/machines/{machineId}/tags")
    public ResponseEntity<ApiResponse<List<TagMappingDto>>> getTagMappings(
            @PathVariable UUID machineId
    ) {
        List<TagMappingDto> mappings = telemetryService.getTagMappings(machineId);
        return ResponseEntity.ok(ApiResponse.ok(mappings));
    }

    @DeleteMapping("/machines/{machineId}/tags/{mappingId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteTagMapping(
            @PathVariable UUID machineId,
            @PathVariable UUID mappingId,
            @AuthenticationPrincipal User actor
    ) {
        telemetryService.deleteTagMapping(machineId, mappingId, actor);
        return ResponseEntity.ok(ApiResponse.ok(null, "Tag mapping deleted successfully"));
    }

    @PostMapping("/ingest")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER')")
    public ResponseEntity<ApiResponse<TelemetryIngestResponse>> ingestBatch(
            @Valid @RequestBody TelemetryBatchIngestRequest request,
            @AuthenticationPrincipal User actor
    ) {
        TelemetryIngestResponse response = telemetryService.ingestBatch(request, actor);
        return ResponseEntity.ok(ApiResponse.ok(response, "Telemetry batch ingested"));
    }

    @GetMapping("/machines/{machineId}/live")
    public ResponseEntity<ApiResponse<MachineLiveTelemetryDto>> getLiveTelemetry(
            @PathVariable UUID machineId
    ) {
        MachineLiveTelemetryDto dto = telemetryService.getLiveTelemetry(machineId);
        return ResponseEntity.ok(ApiResponse.ok(dto));
    }

    @GetMapping("/machines/{machineId}/history")
    public ResponseEntity<ApiResponse<List<TelemetryPointDto>>> getTelemetryHistory(
            @PathVariable UUID machineId,
            @RequestParam(defaultValue = "50") int limit
    ) {
        List<TelemetryPointDto> history = telemetryService.getTelemetryHistory(machineId, limit);
        return ResponseEntity.ok(ApiResponse.ok(history));
    }

    @GetMapping("/machines/{machineId}/series")
    public ResponseEntity<ApiResponse<TimeSeriesResponseDto>> getTimeSeries(
            @PathVariable UUID machineId,
            @RequestParam String tag,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.Instant from,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.Instant to,
            @RequestParam(required = false) String bucket
    ) {
        com.factoryos.modules.telemetry.domain.DownsampleBucket downsampleBucket = 
                com.factoryos.modules.telemetry.domain.DownsampleBucket.fromCode(bucket);
        TimeSeriesResponseDto series = downsamplingService.getTimeSeries(machineId, tag, from, to, downsampleBucket);
        return ResponseEntity.ok(ApiResponse.ok(series));
    }

    @PostMapping("/retention/execute")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RetentionExecutionReport>> executeRetention() {
        RetentionExecutionReport report = downsamplingService.executeRetentionPolicy(null, null, null);
        return ResponseEntity.ok(ApiResponse.ok(report, "Telemetry retention executed"));
    }
}
