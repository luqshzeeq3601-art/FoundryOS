package com.factoryos.modules.sop.application;

import com.factoryos.common.dto.ApiResponse;
import com.factoryos.modules.auth.domain.User;
import com.factoryos.modules.sop.domain.SopCategory;
import com.factoryos.modules.sop.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v2/sop")
public class SopController {

    private final SopService sopService;
    private final QualityGateService qualityGateService;

    public SopController(SopService sopService, QualityGateService qualityGateService) {
        this.sopService = sopService;
        this.qualityGateService = qualityGateService;
    }

    @GetMapping("/templates")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<SopDto>>> getAllSops(
            @RequestParam(required = false) String productCode,
            @RequestParam(required = false) SopCategory category
    ) {
        List<SopDto> sops = sopService.getAllSops(productCode, category);
        return ResponseEntity.ok(ApiResponse.ok(sops));
    }

    @GetMapping("/templates/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<SopDto>> getSopById(@PathVariable UUID id) {
        SopDto sop = sopService.getSopById(id);
        return ResponseEntity.ok(ApiResponse.ok(sop));
    }

    @GetMapping("/templates/code/{code}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<SopDto>> getSopByCode(@PathVariable String code) {
        SopDto sop = sopService.getSopByCode(code);
        return ResponseEntity.ok(ApiResponse.ok(sop));
    }

    @PostMapping("/templates")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER')")
    public ResponseEntity<ApiResponse<SopDto>> createSop(
            @Valid @RequestBody SopCreateRequestDto request,
            @AuthenticationPrincipal User actor
    ) {
        SopDto sop = sopService.createSop(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(sop));
    }

    @PostMapping("/sessions/start")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN', 'OPERATOR')")
    public ResponseEntity<ApiResponse<SopExecutionSessionDto>> startSession(
            @Valid @RequestBody StartSopSessionRequestDto request,
            @AuthenticationPrincipal User actor
    ) {
        SopExecutionSessionDto session = sopService.startExecutionSession(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(session));
    }

    @GetMapping("/sessions/{sessionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<SopExecutionSessionDto>> getSessionById(@PathVariable UUID sessionId) {
        SopExecutionSessionDto session = sopService.getSessionById(sessionId);
        return ResponseEntity.ok(ApiResponse.ok(session));
    }

    @GetMapping("/sessions/order/{orderId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<SopExecutionSessionDto>>> getSessionsForOrder(@PathVariable UUID orderId) {
        List<SopExecutionSessionDto> sessions = sopService.getSessionsForOrder(orderId);
        return ResponseEntity.ok(ApiResponse.ok(sessions));
    }

    @PostMapping("/sessions/{sessionId}/steps")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN', 'OPERATOR')")
    public ResponseEntity<ApiResponse<SopStepExecutionRecordDto>> recordStep(
            @PathVariable UUID sessionId,
            @Valid @RequestBody RecordStepExecutionRequestDto request,
            @AuthenticationPrincipal User actor
    ) {
        SopStepExecutionRecordDto record = sopService.recordStepExecution(sessionId, request, actor);
        return ResponseEntity.ok(ApiResponse.ok(record));
    }

    @PostMapping("/sessions/{sessionId}/sign-off")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN')")
    public ResponseEntity<ApiResponse<SopExecutionSessionDto>> signOffSession(
            @PathVariable UUID sessionId,
            @Valid @RequestBody QualitySignOffRequestDto request,
            @AuthenticationPrincipal User actor
    ) {
        SopExecutionSessionDto session = sopService.completeExecutionSession(sessionId, request, actor);
        return ResponseEntity.ok(ApiResponse.ok(session));
    }

    @GetMapping("/gates/order/{orderId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<QualityGateStatusDto>> getGateStatus(@PathVariable UUID orderId) {
        QualityGateStatusDto status = qualityGateService.getGateStatusForOrder(orderId);
        return ResponseEntity.ok(ApiResponse.ok(status));
    }

    @PostMapping("/gates/order/{orderId}/sign-off")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN')")
    public ResponseEntity<ApiResponse<QualityGateStatusDto>> signOffGate(
            @PathVariable UUID orderId,
            @Valid @RequestBody QualitySignOffRequestDto request,
            @AuthenticationPrincipal User actor
    ) {
        QualityGateStatusDto status = qualityGateService.signOffGate(orderId, request, actor);
        return ResponseEntity.ok(ApiResponse.ok(status));
    }
}
