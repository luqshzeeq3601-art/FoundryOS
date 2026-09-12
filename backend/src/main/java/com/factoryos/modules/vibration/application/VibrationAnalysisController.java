package com.factoryos.modules.vibration.application;

import com.factoryos.common.dto.ApiResponse;
import com.factoryos.modules.auth.domain.User;
import com.factoryos.modules.tenant.context.TenantContextHolder;
import com.factoryos.modules.vibration.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v2/vibration")
public class VibrationAnalysisController {

    private final MachineHealthScoringService scoringService;

    public VibrationAnalysisController(MachineHealthScoringService scoringService) {
        this.scoringService = scoringService;
    }

    @PostMapping("/analyze")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN')")
    public ResponseEntity<ApiResponse<MachineHealthAssessmentDto>> analyzeBurst(
            @Valid @RequestBody VibrationBurstIngestDto request,
            @AuthenticationPrincipal User actor
    ) {
        MachineHealthAssessmentDto assessment = scoringService.processAndAssessBurst(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(assessment));
    }

    @PostMapping("/simulate-burst")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN')")
    public ResponseEntity<ApiResponse<MachineHealthAssessmentDto>> simulateBurst(
            @Valid @RequestBody SimulateBurstRequestDto request,
            @AuthenticationPrincipal User actor
    ) {
        MachineHealthAssessmentDto assessment = scoringService.simulateBurst(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(assessment));
    }

    @GetMapping("/machines/{machineId}/spectrum")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<FftSpectrumDto>> getLatestSpectrum(@PathVariable UUID machineId) {
        FftSpectrumDto spectrum = scoringService.getLatestSpectrum(machineId);
        return ResponseEntity.ok(ApiResponse.ok(spectrum));
    }

    @GetMapping("/machines/{machineId}/health")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<MachineHealthAssessmentDto>> getLatestHealthAssessment(@PathVariable UUID machineId) {
        MachineHealthAssessmentDto assessment = scoringService.getLatestAssessment(machineId);
        return ResponseEntity.ok(ApiResponse.ok(assessment));
    }

    @GetMapping("/machines/{machineId}/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<MachineHealthAssessmentDto>>> getHealthHistory(@PathVariable UUID machineId) {
        List<MachineHealthAssessmentDto> history = scoringService.getAssessmentHistory(machineId);
        return ResponseEntity.ok(ApiResponse.ok(history));
    }

    @GetMapping("/fleet/health-summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN', 'VIEWER')")
    public ResponseEntity<ApiResponse<FleetHealthSummaryDto>> getFleetHealthSummary(
            @RequestParam(required = false) UUID plantId
    ) {
        UUID effectivePlantId = plantId != null ? plantId : TenantContextHolder.getCurrentPlantId();
        FleetHealthSummaryDto summary = scoringService.getFleetHealthSummary(effectivePlantId);
        return ResponseEntity.ok(ApiResponse.ok(summary));
    }
}
