package com.factoryos.modules.erp.application;

import com.factoryos.modules.erp.dto.*;
import com.factoryos.modules.production.domain.ProductionOrder;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v2/erp")
public class ErpIntegrationController {

    private final ErpSyncService erpSyncService;

    public ErpIntegrationController(ErpSyncService erpSyncService) {
        this.erpSyncService = erpSyncService;
    }

    @GetMapping("/connectors")
    public ResponseEntity<List<ErpConnectorDto>> getConnectors() {
        return ResponseEntity.ok(erpSyncService.getAllConnectors());
    }

    @GetMapping("/connectors/{id}")
    public ResponseEntity<ErpConnectorDto> getConnector(@PathVariable UUID id) {
        return ResponseEntity.ok(erpSyncService.getConnectorById(id));
    }

    @PostMapping("/connectors")
    public ResponseEntity<ErpConnectorDto> createConnector(@Valid @RequestBody CreateErpConnectorRequest request) {
        ErpConnectorDto created = erpSyncService.createConnector(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/connectors/{id}/test")
    public ResponseEntity<Map<String, Object>> testConnector(@PathVariable UUID id) {
        boolean ok = erpSyncService.testConnector(id);
        return ResponseEntity.ok(Map.of(
                "connectorId", id,
                "success", ok,
                "status", ok ? "HEALTHY" : "OFFLINE"
        ));
    }

    @PostMapping("/connectors/{id}/sync-inbound")
    public ResponseEntity<Map<String, Object>> syncInbound(@PathVariable UUID id) {
        List<ProductionOrder> syncedOrders = erpSyncService.pollReleasedOrders(id);
        return ResponseEntity.ok(Map.of(
                "connectorId", id,
                "syncedCount", syncedOrders.size(),
                "orders", syncedOrders.stream().map(ProductionOrder::getOrderNumber).toList()
        ));
    }

    @PostMapping("/orders/{orderId}/confirm")
    public ResponseEntity<ErpOrderConfirmationDto> submitConfirmation(
            @PathVariable UUID orderId,
            @RequestBody(required = false) Map<String, Object> req) {
        int yieldQty = req != null && req.containsKey("confirmedGoodQty") ? Integer.parseInt(req.get("confirmedGoodQty").toString()) : (req != null && req.containsKey("yieldQuantity") ? Integer.parseInt(req.get("yieldQuantity").toString()) : 0);
        int scrapQty = req != null && req.containsKey("confirmedScrapQty") ? Integer.parseInt(req.get("confirmedScrapQty").toString()) : (req != null && req.containsKey("scrapQuantity") ? Integer.parseInt(req.get("scrapQuantity").toString()) : 0);
        String scrapReason = req != null && req.containsKey("scrapReason") ? (String) req.get("scrapReason") : (req != null && req.containsKey("scrapReasonCode") ? (String) req.get("scrapReasonCode") : null);
        double laborHours = req != null && req.containsKey("laborHours") ? Double.parseDouble(req.get("laborHours").toString()) : 0.0;
        double machineHours = req != null && req.containsKey("machineHours") ? Double.parseDouble(req.get("machineHours").toString()) : 0.0;

        ErpOrderConfirmationDto result = erpSyncService.submitOrderConfirmation(orderId, yieldQty, scrapQty, scrapReason, laborHours, machineHours);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/sync-logs")
    public ResponseEntity<List<ErpSyncLogDto>> getSyncLogs() {
        return ResponseEntity.ok(erpSyncService.getRecentSyncLogs());
    }

    @GetMapping("/confirmations")
    public ResponseEntity<List<ErpOrderConfirmationDto>> getConfirmations() {
        return ResponseEntity.ok(erpSyncService.getRecentConfirmations());
    }
}
