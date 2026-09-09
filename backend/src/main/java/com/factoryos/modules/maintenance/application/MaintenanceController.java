package com.factoryos.modules.maintenance.application;

import com.factoryos.common.dto.ApiResponse;
import com.factoryos.common.dto.PagedResponse;
import com.factoryos.modules.auth.domain.User;
import com.factoryos.modules.maintenance.domain.MaintenancePriority;
import com.factoryos.modules.maintenance.domain.MaintenanceStatus;
import com.factoryos.modules.maintenance.dto.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/maintenance-work-orders")
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    public MaintenanceController(MaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<PagedResponse<WorkOrderDto>>> getWorkOrders(
            @RequestParam(required = false) UUID machineId,
            @RequestParam(required = false) MaintenancePriority priority,
            @RequestParam(required = false) MaintenanceStatus status,
            @RequestParam(required = false) UUID assignedToId,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                maintenanceService.getWorkOrders(machineId, priority, status, assignedToId, search, pageable)
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<WorkOrderDto>> getWorkOrderById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(maintenanceService.getWorkOrderById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN')")
    public ResponseEntity<ApiResponse<WorkOrderDto>> createWorkOrder(
            @Valid @RequestBody CreateWorkOrderRequest request,
            @AuthenticationPrincipal User actor
    ) {
        WorkOrderDto dto = maintenanceService.createWorkOrder(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER')")
    public ResponseEntity<ApiResponse<WorkOrderDto>> updateWorkOrder(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateWorkOrderRequest request,
            @AuthenticationPrincipal User actor
    ) {
        return ResponseEntity.ok(ApiResponse.ok(maintenanceService.updateWorkOrder(id, request, actor)));
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER')")
    public ResponseEntity<ApiResponse<WorkOrderDto>> assignWorkOrder(
            @PathVariable UUID id,
            @Valid @RequestBody AssignWorkOrderRequest request,
            @AuthenticationPrincipal User actor
    ) {
        return ResponseEntity.ok(ApiResponse.ok(maintenanceService.assignWorkOrder(id, request, actor)));
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN')")
    public ResponseEntity<ApiResponse<WorkOrderDto>> startWorkOrder(
            @PathVariable UUID id,
            @RequestParam Long expectedVersion,
            @AuthenticationPrincipal User actor
    ) {
        return ResponseEntity.ok(ApiResponse.ok(maintenanceService.startWorkOrder(id, expectedVersion, actor)));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN')")
    public ResponseEntity<ApiResponse<WorkOrderDto>> completeWorkOrder(
            @PathVariable UUID id,
            @Valid @RequestBody CompleteWorkOrderRequest request,
            @AuthenticationPrincipal User actor
    ) {
        return ResponseEntity.ok(ApiResponse.ok(maintenanceService.completeWorkOrder(id, request, actor)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER')")
    public ResponseEntity<ApiResponse<WorkOrderDto>> cancelWorkOrder(
            @PathVariable UUID id,
            @Valid @RequestBody CancelWorkOrderRequest request,
            @AuthenticationPrincipal User actor
    ) {
        return ResponseEntity.ok(ApiResponse.ok(maintenanceService.cancelWorkOrder(id, request, actor)));
    }
}
