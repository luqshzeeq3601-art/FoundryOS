package com.factoryos.modules.production.application;

import com.factoryos.common.dto.ApiResponse;
import com.factoryos.common.dto.PagedResponse;
import com.factoryos.modules.auth.domain.User;
import com.factoryos.modules.production.domain.ProductionOrderStatus;
import com.factoryos.modules.production.dto.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/production-orders")
public class ProductionOrderController {

    private final ProductionOrderService productionOrderService;

    public ProductionOrderController(ProductionOrderService productionOrderService) {
        this.productionOrderService = productionOrderService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<PagedResponse<ProductionOrderDto>>> getOrders(
            @RequestParam(required = false) UUID machineId,
            @RequestParam(required = false) ProductionOrderStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                productionOrderService.getProductionOrders(machineId, status, search, pageable)
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<ProductionOrderDto>> getOrderById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(productionOrderService.getProductionOrderById(id)));
    }

    @GetMapping("/active/machine/{machineId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<ProductionOrderDto>> getActiveOrderForMachine(@PathVariable UUID machineId) {
        Optional<ProductionOrderDto> active = productionOrderService.getActiveOrderForMachine(machineId);
        return active.map(dto -> ResponseEntity.ok(ApiResponse.ok(dto)))
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.ok(null)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER')")
    public ResponseEntity<ApiResponse<ProductionOrderDto>> createOrder(
            @Valid @RequestBody CreateProductionOrderRequest request,
            @AuthenticationPrincipal User actor
    ) {
        ProductionOrderDto dto = productionOrderService.createProductionOrder(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER')")
    public ResponseEntity<ApiResponse<ProductionOrderDto>> updateOrder(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProductionOrderRequest request,
            @AuthenticationPrincipal User actor
    ) {
        return ResponseEntity.ok(ApiResponse.ok(productionOrderService.updateProductionOrder(id, request, actor)));
    }

    @PatchMapping("/{id}/progress")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'OPERATOR')")
    public ResponseEntity<ApiResponse<ProductionOrderDto>> updateProgress(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProductionProgressRequest request,
            @AuthenticationPrincipal User actor
    ) {
        return ResponseEntity.ok(ApiResponse.ok(productionOrderService.updateProgress(id, request, actor)));
    }

    @PostMapping("/{id}/transition")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'OPERATOR')")
    public ResponseEntity<ApiResponse<ProductionOrderDto>> transitionStatus(
            @PathVariable UUID id,
            @Valid @RequestBody TransitionOrderStatusRequest request,
            @AuthenticationPrincipal User actor
    ) {
        return ResponseEntity.ok(ApiResponse.ok(productionOrderService.transitionStatus(id, request, actor)));
    }
}
