package com.factoryos.modules.downtime.application;

import com.factoryos.common.dto.ApiResponse;
import com.factoryos.modules.auth.domain.User;
import com.factoryos.modules.downtime.dto.AcknowledgeRootCauseRequest;
import com.factoryos.modules.downtime.dto.AutomatedEvaluationResultDto;
import com.factoryos.modules.downtime.dto.DowntimeEventDto;
import com.factoryos.modules.downtime.dto.MicroStopSummaryDto;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v2/downtime")
public class AutomatedDowntimeController {

    private final AutomatedDowntimeDetectionService automatedDowntimeDetectionService;

    public AutomatedDowntimeController(AutomatedDowntimeDetectionService automatedDowntimeDetectionService) {
        this.automatedDowntimeDetectionService = automatedDowntimeDetectionService;
    }

    @GetMapping("/pending-root-causes")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<DowntimeEventDto>>> getPendingRootCauses() {
        List<DowntimeEventDto> pending = automatedDowntimeDetectionService.getPendingRootCauseEvents();
        return ResponseEntity.ok(ApiResponse.ok(pending));
    }

    @PostMapping("/{id}/acknowledge-root-cause")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN', 'OPERATOR')")
    public ResponseEntity<ApiResponse<DowntimeEventDto>> acknowledgeRootCause(
            @PathVariable UUID id,
            @Valid @RequestBody AcknowledgeRootCauseRequest request,
            @AuthenticationPrincipal User actor
    ) {
        DowntimeEventDto dto = automatedDowntimeDetectionService.acknowledgeRootCause(id, request, actor);
        return ResponseEntity.ok(ApiResponse.ok(dto, "Root cause acknowledged successfully"));
    }

    @GetMapping("/micro-stops")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<MicroStopSummaryDto>> getMicroStopSummary(
            @RequestParam(required = false) UUID machineId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        MicroStopSummaryDto summary = automatedDowntimeDetectionService.getMicroStopSummary(machineId, from, to);
        return ResponseEntity.ok(ApiResponse.ok(summary));
    }

    @PostMapping("/evaluate/{machineId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'OPERATOR')")
    public ResponseEntity<ApiResponse<AutomatedEvaluationResultDto>> evaluateStream(
            @PathVariable UUID machineId,
            @RequestParam(defaultValue = "0.0") double spindleSpeed,
            @RequestParam(defaultValue = "0.0") double cycleCountDelta
    ) {
        AutomatedEvaluationResultDto result = automatedDowntimeDetectionService.evaluateMachineStream(
                machineId,
                spindleSpeed,
                cycleCountDelta,
                Instant.now()
        );
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
