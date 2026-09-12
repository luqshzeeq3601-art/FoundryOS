package com.factoryos.modules.materials.application;

import com.factoryos.common.dto.ApiResponse;
import com.factoryos.modules.auth.domain.User;
import com.factoryos.modules.materials.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v2/materials")
public class MaterialController {

    private final MaterialBackflushingService materialBackflushingService;

    public MaterialController(MaterialBackflushingService materialBackflushingService) {
        this.materialBackflushingService = materialBackflushingService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<MaterialDto>>> getAllMaterials() {
        return ResponseEntity.ok(ApiResponse.ok(materialBackflushingService.getAllMaterials()));
    }

    @GetMapping("/bom/{productCode}/explosion")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<BomExplosionDto>> getBomExplosion(
            @PathVariable String productCode,
            @RequestParam(defaultValue = "100") int plannedQuantity
    ) {
        return ResponseEntity.ok(ApiResponse.ok(materialBackflushingService.getBomExplosion(productCode, plannedQuantity)));
    }

    @PostMapping("/orders/{orderId}/record-output")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'OPERATOR')")
    public ResponseEntity<ApiResponse<List<MaterialConsumptionRecordDto>>> recordProductionOutput(
            @PathVariable UUID orderId,
            @Valid @RequestBody RecordProductionOutputRequest request,
            @AuthenticationPrincipal User actor
    ) {
        List<MaterialConsumptionRecordDto> records = materialBackflushingService.recordProductionOutputAndBackflush(orderId, request, actor);
        return ResponseEntity.ok(ApiResponse.ok(records));
    }

    @GetMapping("/consumption")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<MaterialConsumptionRecordDto>>> getConsumptionRecords(
            @RequestParam(required = false) UUID orderId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(materialBackflushingService.getRecentConsumptionRecords(orderId)));
    }

    @GetMapping("/variance-alerts")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<MaterialConsumptionRecordDto>>> getVarianceAlerts() {
        return ResponseEntity.ok(ApiResponse.ok(materialBackflushingService.getVarianceAlerts()));
    }
}
