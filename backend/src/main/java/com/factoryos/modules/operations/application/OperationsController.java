package com.factoryos.modules.operations.application;

import com.factoryos.common.dto.ApiResponse;
import com.factoryos.modules.auth.domain.User;
import com.factoryos.modules.downtime.dto.DowntimeEventDto;
import com.factoryos.modules.operations.dto.BreakdownReportRequest;
import com.factoryos.modules.operations.dto.ResolveBreakdownRequest;
import com.factoryos.modules.production.dto.ProductionOrderDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/operations")
public class OperationsController {

    private final OperationsCoordinator operationsCoordinator;

    public OperationsController(OperationsCoordinator operationsCoordinator) {
        this.operationsCoordinator = operationsCoordinator;
    }

    @PostMapping("/breakdown")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN', 'OPERATOR')")
    public ResponseEntity<ApiResponse<DowntimeEventDto>> reportBreakdown(
            @Valid @RequestBody BreakdownReportRequest request,
            @AuthenticationPrincipal User actor
    ) {
        DowntimeEventDto dto = operationsCoordinator.reportBreakdown(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(dto));
    }

    @PostMapping("/machines/{machineId}/resolve-breakdown")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN', 'OPERATOR')")
    public ResponseEntity<ApiResponse<DowntimeEventDto>> resolveBreakdown(
            @PathVariable UUID machineId,
            @Valid @RequestBody ResolveBreakdownRequest request,
            @AuthenticationPrincipal User actor
    ) {
        return ResponseEntity.ok(ApiResponse.ok(operationsCoordinator.resolveBreakdown(machineId, request, actor)));
    }

    @PostMapping("/production-orders/{orderId}/start")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'OPERATOR')")
    public ResponseEntity<ApiResponse<ProductionOrderDto>> startProductionRun(
            @PathVariable UUID orderId,
            @RequestParam Long expectedVersion,
            @AuthenticationPrincipal User actor
    ) {
        return ResponseEntity.ok(ApiResponse.ok(operationsCoordinator.startProductionRun(orderId, expectedVersion, actor)));
    }
}
