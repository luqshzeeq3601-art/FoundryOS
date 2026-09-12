package com.factoryos.modules.edge.application;

import com.factoryos.common.dto.ApiResponse;
import com.factoryos.modules.edge.dto.*;
import com.factoryos.modules.edge.service.EdgeGatewayService;
import com.factoryos.modules.edge.service.EdgeStoreAndForwardSyncService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v2/edge")
public class EdgeController {

    private final EdgeGatewayService gatewayService;
    private final EdgeStoreAndForwardSyncService syncService;

    public EdgeController(EdgeGatewayService gatewayService, EdgeStoreAndForwardSyncService syncService) {
        this.gatewayService = gatewayService;
        this.syncService = syncService;
    }

    @GetMapping("/gateways")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<EdgeGatewayDto>>> getGateways(
            @RequestParam(required = false) UUID plantId
    ) {
        List<EdgeGatewayDto> gateways = plantId != null
                ? gatewayService.getGatewaysByPlant(plantId)
                : gatewayService.getAllGateways();
        return ResponseEntity.ok(ApiResponse.ok(gateways));
    }

    @GetMapping("/gateways/{code}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN', 'VIEWER')")
    public ResponseEntity<ApiResponse<EdgeGatewayDto>> getGatewayByCode(
            @PathVariable String code
    ) {
        EdgeGatewayDto gateway = gatewayService.getGatewayByCode(code);
        return ResponseEntity.ok(ApiResponse.ok(gateway));
    }

    @PostMapping("/gateways")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER')")
    public ResponseEntity<ApiResponse<EdgeGatewayDto>> createGateway(
            @Valid @RequestBody EdgeGatewayCreateRequestDto request
    ) {
        EdgeGatewayDto gateway = gatewayService.createGateway(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(gateway));
    }

    @PostMapping("/gateways/{code}/heartbeat")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'TECHNICIAN')")
    public ResponseEntity<ApiResponse<EdgeGatewayDto>> recordHeartbeat(
            @PathVariable String code,
            @RequestBody(required = false) EdgeGatewayHeartbeatRequestDto request
    ) {
        EdgeGatewayDto gateway = gatewayService.recordHeartbeat(code, request);
        return ResponseEntity.ok(ApiResponse.ok(gateway));
    }

    @GetMapping("/gateways/{code}/manifest")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN', 'VIEWER')")
    public ResponseEntity<ApiResponse<EdgeOfflineCacheManifestDto>> getOfflineCacheManifest(
            @PathVariable String code
    ) {
        EdgeOfflineCacheManifestDto manifest = gatewayService.generateOfflineCacheManifest(code);
        return ResponseEntity.ok(ApiResponse.ok(manifest));
    }

    @PostMapping("/sync/batch")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN')")
    public ResponseEntity<ApiResponse<EdgeSyncBatchResultDto>> processSyncBatch(
            @Valid @RequestBody EdgeSyncBatchRequestDto request
    ) {
        EdgeSyncBatchResultDto result = syncService.processSyncBatch(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/batches")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<EdgeSyncBatchResultDto>>> getRecentBatches() {
        List<EdgeSyncBatchResultDto> batches = syncService.getRecentBatches();
        return ResponseEntity.ok(ApiResponse.ok(batches));
    }

    @GetMapping("/gateways/{code}/batches")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<EdgeSyncBatchResultDto>>> getGatewayBatches(
            @PathVariable String code
    ) {
        List<EdgeSyncBatchResultDto> batches = syncService.getSyncBatchesByGateway(code);
        return ResponseEntity.ok(ApiResponse.ok(batches));
    }

    @GetMapping("/batches/{batchId}/transactions")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<EdgeTransactionLogDto>>> getBatchTransactions(
            @PathVariable UUID batchId
    ) {
        List<EdgeTransactionLogDto> txs = syncService.getTransactionsForBatch(batchId);
        return ResponseEntity.ok(ApiResponse.ok(txs));
    }

    @GetMapping("/transactions/recent")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<EdgeTransactionLogDto>>> getRecentTransactions() {
        List<EdgeTransactionLogDto> txs = syncService.getRecentTransactions();
        return ResponseEntity.ok(ApiResponse.ok(txs));
    }

    @PostMapping("/simulate-disconnect")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER')")
    public ResponseEntity<ApiResponse<EdgeSyncBatchResultDto>> simulateDisconnect(
            @RequestBody SimulateDisconnectRequestDto request
    ) {
        EdgeSyncBatchResultDto result = syncService.simulate4HourDisconnect(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
