package com.factoryos.modules.edge.service;

import com.factoryos.modules.audit.application.AuditRecordingService;
import com.factoryos.modules.barcode.domain.BarcodeScanLog;
import com.factoryos.modules.barcode.domain.BarcodeType;
import com.factoryos.modules.barcode.domain.BarcodeValidationStatus;
import com.factoryos.modules.barcode.domain.ResolvedEntityType;
import com.factoryos.modules.barcode.repository.BarcodeScanLogRepository;
import com.factoryos.modules.downtime.domain.DowntimeEvent;
import com.factoryos.modules.downtime.domain.DowntimeReasonCode;
import com.factoryos.modules.downtime.domain.DowntimeTriggerSource;
import com.factoryos.modules.downtime.repository.DowntimeEventRepository;
import com.factoryos.modules.edge.domain.*;
import com.factoryos.modules.edge.dto.*;
import com.factoryos.modules.edge.repository.EdgeGatewayRepository;
import com.factoryos.modules.edge.repository.EdgeOfflineSyncBatchRepository;
import com.factoryos.modules.edge.repository.EdgeOfflineTransactionLogRepository;
import com.factoryos.modules.machine.domain.Machine;
import com.factoryos.modules.machine.repository.MachineRepository;
import com.factoryos.modules.production.domain.ProductionOrder;
import com.factoryos.modules.production.repository.ProductionOrderRepository;
import com.factoryos.modules.tenant.domain.Plant;
import com.factoryos.modules.tenant.repository.PlantRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class EdgeStoreAndForwardSyncService {

    private static final Logger log = LoggerFactory.getLogger(EdgeStoreAndForwardSyncService.class);

    private final EdgeGatewayRepository edgeGatewayRepository;
    private final EdgeOfflineSyncBatchRepository edgeOfflineSyncBatchRepository;
    private final EdgeOfflineTransactionLogRepository edgeOfflineTransactionLogRepository;
    private final ProductionOrderRepository productionOrderRepository;
    private final BarcodeScanLogRepository barcodeScanLogRepository;
    private final DowntimeEventRepository downtimeEventRepository;
    private final MachineRepository machineRepository;
    private final PlantRepository plantRepository;
    private final AuditRecordingService auditRecordingService;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public EdgeStoreAndForwardSyncService(EdgeGatewayRepository edgeGatewayRepository,
                                          EdgeOfflineSyncBatchRepository edgeOfflineSyncBatchRepository,
                                          EdgeOfflineTransactionLogRepository edgeOfflineTransactionLogRepository,
                                          ProductionOrderRepository productionOrderRepository,
                                          BarcodeScanLogRepository barcodeScanLogRepository,
                                          DowntimeEventRepository downtimeEventRepository,
                                          MachineRepository machineRepository,
                                          PlantRepository plantRepository,
                                          AuditRecordingService auditRecordingService) {
        this.edgeGatewayRepository = edgeGatewayRepository;
        this.edgeOfflineSyncBatchRepository = edgeOfflineSyncBatchRepository;
        this.edgeOfflineTransactionLogRepository = edgeOfflineTransactionLogRepository;
        this.productionOrderRepository = productionOrderRepository;
        this.barcodeScanLogRepository = barcodeScanLogRepository;
        this.downtimeEventRepository = downtimeEventRepository;
        this.machineRepository = machineRepository;
        this.plantRepository = plantRepository;
        this.auditRecordingService = auditRecordingService;
    }

    public EdgeSyncBatchResultDto processSyncBatch(EdgeSyncBatchRequestDto batchRequest) {
        log.info("Processing Edge Sync Batch: {} from Gateway: {}", batchRequest.getBatchId(), batchRequest.getGatewayCode());

        EdgeGateway gateway = edgeGatewayRepository.findByGatewayCodeAndIsDeletedFalse(batchRequest.getGatewayCode().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Edge Gateway not found: " + batchRequest.getGatewayCode()));

        Plant plant = plantRepository.findByIdAndIsDeletedFalse(batchRequest.getPlantId())
                .orElse(gateway.getPlant());

        // Check if batch already reconciled
        Optional<EdgeOfflineSyncBatch> existingBatchOpt = edgeOfflineSyncBatchRepository.findByBatchId(batchRequest.getBatchId());
        if (existingBatchOpt.isPresent() && existingBatchOpt.get().getSyncStatus() == SyncStatus.RECONCILED) {
            log.warn("Batch {} already reconciled. Returning existing outcome idempotently.", batchRequest.getBatchId());
            return toResultDto(existingBatchOpt.get(), 0, 0);
        }

        EdgeOfflineSyncBatch batch = existingBatchOpt.orElseGet(() -> {
            EdgeOfflineSyncBatch b = new EdgeOfflineSyncBatch();
            b.setBatchId(batchRequest.getBatchId());
            b.setGateway(gateway);
            b.setPlant(plant);
            b.setSequenceStart(batchRequest.getSequenceStart());
            b.setSequenceEnd(batchRequest.getSequenceEnd());
            b.setTotalRecords(batchRequest.getTransactions() != null ? batchRequest.getTransactions().size() : 0);
            b.setDisconnectedAt(batchRequest.getDisconnectedAt() != null ? batchRequest.getDisconnectedAt() : Instant.now().minus(4, ChronoUnit.HOURS));
            b.setReconnectedAt(batchRequest.getReconnectedAt() != null ? batchRequest.getReconnectedAt() : Instant.now());
            b.setSyncStatus(SyncStatus.PROCESSING);
            return edgeOfflineSyncBatchRepository.save(b);
        });

        int processedCount = 0;
        int duplicateIgnoredCount = 0;
        int failedCount = 0;
        int conflictResolvedCount = 0;

        List<EdgeTransactionRecordDto> sortedTransactions = batchRequest.getTransactions() != null
                ? batchRequest.getTransactions().stream()
                .sorted(Comparator.comparingLong(EdgeTransactionRecordDto::getSequenceId))
                .collect(Collectors.toList())
                : Collections.emptyList();

        for (EdgeTransactionRecordDto record : sortedTransactions) {
            // Idempotency check
            if (edgeOfflineTransactionLogRepository.existsByIdempotencyKey(record.getIdempotencyKey())) {
                duplicateIgnoredCount++;
                log.debug("Transaction {} already exists, ignoring duplicate", record.getIdempotencyKey());
                continue;
            }

            EdgeOfflineTransactionLog txLog = new EdgeOfflineTransactionLog();
            txLog.setBatch(batch);
            txLog.setGateway(gateway);
            txLog.setPlant(plant);
            txLog.setSequenceId(record.getSequenceId());
            txLog.setIdempotencyKey(record.getIdempotencyKey());
            txLog.setTransactionType(record.getTransactionType());
            txLog.setEntityType(record.getEntityType());
            txLog.setEntityId(record.getEntityId());
            txLog.setPayloadJson(record.getPayloadJson());
            txLog.setVectorClockVersion(record.getVectorClockVersion());
            txLog.setRecordedAt(record.getRecordedAt() != null ? record.getRecordedAt() : Instant.now());
            txLog.setSyncedAt(Instant.now());

            try {
                processIndividualTransaction(record, txLog, plant);
                if (txLog.getExecutionStatus() == ExecutionStatus.CONFLICT_RESOLVED) {
                    conflictResolvedCount++;
                }
                processedCount++;
            } catch (Exception e) {
                log.error("Failed to process transaction: {}", record.getIdempotencyKey(), e);
                txLog.setExecutionStatus(ExecutionStatus.FAILED);
                txLog.setConflictResolutionNote("Processing error: " + e.getMessage());
                failedCount++;
            }

            edgeOfflineTransactionLogRepository.save(txLog);
        }

        batch.setProcessedRecords(processedCount);
        batch.setFailedRecords(failedCount);
        batch.setSyncStatus(failedCount > 0 && processedCount == 0 ? SyncStatus.FAILED :
                (failedCount > 0 ? SyncStatus.PARTIAL : SyncStatus.RECONCILED));
        batch.setReconciledAt(Instant.now());
        batch.setReconciliationNotes(String.format("Reconciled %d txs (%d duplicates skipped, %d conflicts resolved, %d failed)",
                processedCount, duplicateIgnoredCount, conflictResolvedCount, failedCount));

        EdgeOfflineSyncBatch savedBatch = edgeOfflineSyncBatchRepository.save(batch);

        // Update Gateway metadata
        gateway.setStatus("ONLINE");
        gateway.setLastSyncAt(Instant.now());
        gateway.setLastHeartbeatAt(Instant.now());
        gateway.setLastSyncSequenceId(Math.max(gateway.getLastSyncSequenceId(), batchRequest.getSequenceEnd()));
        gateway.setUpdatedAt(Instant.now());
        edgeGatewayRepository.save(gateway);

        // Audit Record
        try {
            Map<String, Object> auditDetails = new HashMap<>();
            auditDetails.put("batchId", savedBatch.getBatchId());
            auditDetails.put("gatewayCode", gateway.getGatewayCode());
            auditDetails.put("processedRecords", processedCount);
            auditDetails.put("duplicateIgnoredRecords", duplicateIgnoredCount);
            auditDetails.put("status", savedBatch.getSyncStatus().name());
            auditRecordingService.record(null, "EDGE_STORE_AND_FORWARD_SYNC", "EdgeOfflineSyncBatch",
                    savedBatch.getId(), Collections.emptyMap(), auditDetails);
        } catch (Exception ex) {
            log.warn("Could not record audit log for batch sync: {}", ex.getMessage());
        }

        return toResultDto(savedBatch, duplicateIgnoredCount, conflictResolvedCount);
    }

    private void processIndividualTransaction(EdgeTransactionRecordDto record, EdgeOfflineTransactionLog txLog, Plant plant) {
        TransactionType type = record.getTransactionType();
        String json = record.getPayloadJson();

        switch (type) {
            case PRODUCTION_OUTPUT:
                reconcileProductionOutput(record, txLog, json);
                break;
            case BARCODE_SCAN:
                reconcileBarcodeScan(record, txLog, json);
                break;
            case DOWNTIME_EVENT:
                reconcileDowntime(record, txLog, json, plant);
                break;
            case OPERATOR_ACTION:
            case MACHINE_STATE_TRANSITION:
            default:
                txLog.setExecutionStatus(ExecutionStatus.PROCESSED);
                txLog.setConflictResolutionNote("Logged state mutation successfully.");
                break;
        }
    }

    private void reconcileProductionOutput(EdgeTransactionRecordDto record, EdgeOfflineTransactionLog txLog, String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            int deltaGood = root.path("deltaGood").asInt(root.path("goodCount").asInt(0));
            int deltaScrap = root.path("deltaScrap").asInt(root.path("scrapCount").asInt(0));

            UUID orderId = record.getEntityId();
            if (orderId == null && root.has("orderId")) {
                try {
                    orderId = UUID.fromString(root.path("orderId").asText());
                } catch (Exception ignored) {}
            }

            if (orderId != null) {
                Optional<ProductionOrder> orderOpt = productionOrderRepository.findByIdAndIsDeletedFalse(orderId);
                if (orderOpt.isPresent()) {
                    ProductionOrder order = orderOpt.get();
                    int beforeGood = order.getGoodQuantity();
                    int beforeScrap = order.getScrapQuantity();

                    // CRITICAL INVARIANT: Additive Delta reconciliation (no overwriting or loss)
                    order.setGoodQuantity(beforeGood + deltaGood);
                    order.setScrapQuantity(beforeScrap + deltaScrap);
                    order.setUpdatedAt(Instant.now());
                    productionOrderRepository.save(order);

                    txLog.setExecutionStatus(ExecutionStatus.PROCESSED);
                    txLog.setConflictResolutionNote(String.format("Additive delta applied: good +%d (was %d -> %d), scrap +%d (was %d -> %d)",
                            deltaGood, beforeGood, order.getGoodQuantity(), deltaScrap, beforeScrap, order.getScrapQuantity()));
                    return;
                }
            }

            // Fallback: search by orderNumber in payload
            if (root.has("orderNumber")) {
                String orderNumber = root.path("orderNumber").asText();
                Optional<ProductionOrder> orderOpt = productionOrderRepository.findByOrderNumberAndIsDeletedFalse(orderNumber);
                if (orderOpt.isPresent()) {
                    ProductionOrder order = orderOpt.get();
                    int beforeGood = order.getGoodQuantity();
                    int beforeScrap = order.getScrapQuantity();

                    order.setGoodQuantity(beforeGood + deltaGood);
                    order.setScrapQuantity(beforeScrap + deltaScrap);
                    order.setUpdatedAt(Instant.now());
                    productionOrderRepository.save(order);

                    txLog.setEntityId(order.getId());
                    txLog.setExecutionStatus(ExecutionStatus.PROCESSED);
                    txLog.setConflictResolutionNote(String.format("Additive delta applied to %s: good +%d, scrap +%d",
                            orderNumber, deltaGood, deltaScrap));
                    return;
                }
            }

            txLog.setExecutionStatus(ExecutionStatus.CONFLICT_RESOLVED);
            txLog.setConflictResolutionNote("Order not found for production output delta. Recorded delta to transaction log ledger.");
        } catch (Exception e) {
            txLog.setExecutionStatus(ExecutionStatus.CONFLICT_RESOLVED);
            txLog.setConflictResolutionNote("Parse warning for production output: " + e.getMessage());
        }
    }

    private void reconcileBarcodeScan(EdgeTransactionRecordDto record, EdgeOfflineTransactionLog txLog, String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            String payload = root.path("scanPayload").asText(root.path("barcode").asText("OFFLINE-SCAN"));
            String format = root.path("barcodeFormat").asText("DATAMATRIX");

            BarcodeScanLog scanLog = new BarcodeScanLog();
            scanLog.setScanPayload(payload);
            scanLog.setBarcodeFormat(format);
            scanLog.setBarcodeType(BarcodeType.MATERIAL_LOT);
            scanLog.setScannerSource("EDGE_BUFFER_SYNC");
            scanLog.setResolvedEntityType(ResolvedEntityType.MATERIAL_LOT);
            scanLog.setResolvedEntityId(root.path("lotNumber").asText("LOT-BUFFERED"));
            scanLog.setResolvedEntitySummary("Synchronized from offline Edge buffer");
            scanLog.setValidationStatus(BarcodeValidationStatus.VALID);
            scanLog.setBomMatched(true);
            scanLog.setLatencyMs(0L);
            scanLog.setProductionOrderId(record.getEntityId());

            if (root.has("machineId")) {
                try {
                    scanLog.setMachineId(UUID.fromString(root.path("machineId").asText()));
                } catch (Exception ignored) {}
            }

            barcodeScanLogRepository.save(scanLog);

            txLog.setExecutionStatus(ExecutionStatus.PROCESSED);
            txLog.setConflictResolutionNote("Recorded offline barcode scan event into enterprise scan ledger.");
        } catch (Exception e) {
            txLog.setExecutionStatus(ExecutionStatus.CONFLICT_RESOLVED);
            txLog.setConflictResolutionNote("Barcode scan parsing: " + e.getMessage());
        }
    }

    private void reconcileDowntime(EdgeTransactionRecordDto record, EdgeOfflineTransactionLog txLog, String json, Plant plant) {
        try {
            JsonNode root = objectMapper.readTree(json);
            String reason = root.path("reasonCode").asText("TOOLING_JAM");
            String description = root.path("description").asText("Offline Edge recorded downtime event");
            long durationMinutes = root.path("durationMinutes").asLong(15L);

            Instant start = root.has("startTime") ? Instant.parse(root.path("startTime").asText()) : record.getRecordedAt();
            Instant end = root.has("endTime") ? Instant.parse(root.path("endTime").asText()) : start.plus(durationMinutes, ChronoUnit.MINUTES);

            Machine targetMachine = null;
            if (record.getEntityId() != null) {
                targetMachine = machineRepository.findByIdAndIsDeletedFalse(record.getEntityId()).orElse(null);
            }
            if (targetMachine == null && root.has("machineId")) {
                try {
                    targetMachine = machineRepository.findByIdAndIsDeletedFalse(UUID.fromString(root.path("machineId").asText())).orElse(null);
                } catch (Exception ignored) {}
            }
            if (targetMachine == null) {
                List<Machine> plantMachines = machineRepository.findByPlantIdAndIsDeletedFalse(plant.getId());
                if (!plantMachines.isEmpty()) {
                    targetMachine = plantMachines.get(0);
                }
            }

            if (targetMachine != null) {
                DowntimeEvent event = new DowntimeEvent();
                event.setMachine(targetMachine);
                event.setPlant(plant);
                try {
                    event.setReasonCode(DowntimeReasonCode.valueOf(reason.toUpperCase()));
                } catch (Exception e) {
                    event.setReasonCode(DowntimeReasonCode.TOOLING_JAM);
                }
                event.setTriggerSource(DowntimeTriggerSource.AUTOMATED_SENSOR);
                event.setDescription(description);
                event.setStartTime(start);
                event.setEndTime(end);
                event.setResolutionNote("Auto-reconciled from offline Edge buffer with preserved duration.");

                downtimeEventRepository.save(event);

                txLog.setExecutionStatus(ExecutionStatus.PROCESSED);
                txLog.setConflictResolutionNote(String.format("Reconciled downtime on %s (%d mins preserved)",
                        targetMachine.getSerialNumber(), ChronoUnit.MINUTES.between(start, end)));
                return;
            }

            txLog.setExecutionStatus(ExecutionStatus.CONFLICT_RESOLVED);
            txLog.setConflictResolutionNote("Downtime logged without machine association.");
        } catch (Exception e) {
            txLog.setExecutionStatus(ExecutionStatus.CONFLICT_RESOLVED);
            txLog.setConflictResolutionNote("Downtime parsing: " + e.getMessage());
        }
    }

    public EdgeSyncBatchResultDto simulate4HourDisconnect(SimulateDisconnectRequestDto simulationRequest) {
        log.info("Simulating 4-hour WAN disconnect for gateway: {}", simulationRequest.getGatewayCode());

        EdgeGateway gateway = edgeGatewayRepository.findByGatewayCodeAndIsDeletedFalse(simulationRequest.getGatewayCode().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Edge Gateway not found: " + simulationRequest.getGatewayCode()));

        Plant plant = gateway.getPlant();

        // 1. Determine target Production Order
        ProductionOrder targetOrder = null;
        if (simulationRequest.getOrderId() != null) {
            targetOrder = productionOrderRepository.findByIdAndIsDeletedFalse(simulationRequest.getOrderId()).orElse(null);
        }
        if (targetOrder == null) {
            List<ProductionOrder> plantOrders = productionOrderRepository.findByPlantIdAndIsDeletedFalse(plant.getId());
            targetOrder = plantOrders.stream()
                    .filter(o -> !o.isDeleted())
                    .findFirst()
                    .orElse(null);
        }

        if (targetOrder == null) {
            throw new IllegalStateException("No active Production Orders found in plant " + plant.getName() + " to simulate offline buffering.");
        }

        Machine targetMachine = targetOrder.getMachine();
        if (targetMachine == null && simulationRequest.getMachineId() != null) {
            targetMachine = machineRepository.findByIdAndIsDeletedFalse(simulationRequest.getMachineId()).orElse(null);
        }
        if (targetMachine == null) {
            List<Machine> plantMachines = machineRepository.findByPlantIdAndIsDeletedFalse(plant.getId());
            if (!plantMachines.isEmpty()) {
                targetMachine = plantMachines.get(0);
            }
        }

        // Generate synthetic offline transactions over a 4-hour window
        Instant disconnectedAt = Instant.now().minus(4, ChronoUnit.HOURS);
        Instant reconnectedAt = Instant.now();
        String batchId = "SIM-DISCONNECT-4H-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        List<EdgeTransactionRecordDto> transactions = new ArrayList<>();
        long seq = gateway.getLastSyncSequenceId() + 1;

        int totalGood = simulationRequest.getProducedPartsGood() > 0 ? simulationRequest.getProducedPartsGood() : 1200;
        int totalScrap = simulationRequest.getProducedPartsScrap() >= 0 ? simulationRequest.getProducedPartsScrap() : 15;
        int scans = simulationRequest.getBarcodeScans() > 0 ? simulationRequest.getBarcodeScans() : 25;

        // Break production into batches (e.g. 10 part output increments)
        int chunkSize = 20;
        int remainingGood = totalGood;
        int remainingScrap = totalScrap;

        int stepMinutes = 2;
        Instant currentTxTime = disconnectedAt;

        while (remainingGood > 0 || remainingScrap > 0) {
            int curGood = Math.min(remainingGood, chunkSize);
            int curScrap = (remainingScrap > 0 && curGood > 0) ? Math.min(remainingScrap, 1) : 0;
            remainingGood -= curGood;
            remainingScrap -= curScrap;

            EdgeTransactionRecordDto prodTx = new EdgeTransactionRecordDto();
            prodTx.setSequenceId(seq++);
            prodTx.setIdempotencyKey("TX-PROD-" + batchId + "-" + seq);
            prodTx.setTransactionType(TransactionType.PRODUCTION_OUTPUT);
            prodTx.setEntityType("ProductionOrder");
            prodTx.setEntityId(targetOrder.getId());
            prodTx.setRecordedAt(currentTxTime);
            prodTx.setVectorClockVersion(1L);
            prodTx.setPayloadJson(String.format("{\"deltaGood\": %d, \"deltaScrap\": %d, \"orderNumber\": \"%s\", \"machineCode\": \"%s\"}",
                    curGood, curScrap, targetOrder.getOrderNumber(), targetMachine != null ? targetMachine.getSerialNumber() : "MCH-01"));
            transactions.add(prodTx);

            currentTxTime = currentTxTime.plus(stepMinutes, ChronoUnit.MINUTES);
            if (currentTxTime.isAfter(reconnectedAt)) {
                currentTxTime = reconnectedAt.minus(1, ChronoUnit.MINUTES);
            }
        }

        // Add Barcode Scans during disconnect
        for (int i = 0; i < scans; i++) {
            EdgeTransactionRecordDto scanTx = new EdgeTransactionRecordDto();
            scanTx.setSequenceId(seq++);
            scanTx.setIdempotencyKey("TX-SCAN-" + batchId + "-" + seq);
            scanTx.setTransactionType(TransactionType.BARCODE_SCAN);
            scanTx.setEntityType("MaterialLot");
            scanTx.setEntityId(targetOrder.getId());
            scanTx.setRecordedAt(disconnectedAt.plus((long) i * 8, ChronoUnit.MINUTES));
            scanTx.setVectorClockVersion(1L);
            scanTx.setPayloadJson(String.format("{\"scanPayload\": \"LOT-SIM-OFFLINE-%04d\", \"barcodeFormat\": \"DATAMATRIX\", \"lotNumber\": \"LOT-SIM-%04d\", \"machineId\": \"%s\"}",
                    i + 1, i + 1, targetMachine != null ? targetMachine.getId() : UUID.randomUUID()));
            transactions.add(scanTx);
        }

        // Add Downtime event during disconnect (e.g. 15-minute feeder jam)
        EdgeTransactionRecordDto downtimeTx = new EdgeTransactionRecordDto();
        downtimeTx.setSequenceId(seq++);
        downtimeTx.setIdempotencyKey("TX-DOWNTIME-" + batchId + "-" + seq);
        downtimeTx.setTransactionType(TransactionType.DOWNTIME_EVENT);
        downtimeTx.setEntityType("Machine");
        downtimeTx.setEntityId(targetMachine != null ? targetMachine.getId() : null);
        Instant dtStart = disconnectedAt.plus(90, ChronoUnit.MINUTES);
        Instant dtEnd = dtStart.plus(simulationRequest.getDowntimeDurationMinutes(), ChronoUnit.MINUTES);
        downtimeTx.setRecordedAt(dtStart);
        downtimeTx.setVectorClockVersion(1L);
        downtimeTx.setPayloadJson(String.format("{\"reasonCode\": \"TOOLING_JAM\", \"description\": \"%s\", \"startTime\": \"%s\", \"endTime\": \"%s\", \"durationMinutes\": %d, \"machineId\": \"%s\"}",
                simulationRequest.getDowntimeReason(), dtStart.toString(), dtEnd.toString(),
                simulationRequest.getDowntimeDurationMinutes(), targetMachine != null ? targetMachine.getId() : ""));
        transactions.add(downtimeTx);

        // Build batch request
        EdgeSyncBatchRequestDto request = new EdgeSyncBatchRequestDto();
        request.setGatewayCode(gateway.getGatewayCode());
        request.setBatchId(batchId);
        request.setPlantId(plant.getId());
        request.setSequenceStart(gateway.getLastSyncSequenceId() + 1);
        request.setSequenceEnd(seq - 1);
        request.setDisconnectedAt(disconnectedAt);
        request.setReconnectedAt(reconnectedAt);
        request.setTransactions(transactions);

        // Process batch
        return processSyncBatch(request);
    }

    @Transactional(readOnly = true)
    public List<EdgeSyncBatchResultDto> getSyncBatchesByGateway(String gatewayCode) {
        EdgeGateway gateway = edgeGatewayRepository.findByGatewayCodeAndIsDeletedFalse(gatewayCode.toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Edge Gateway not found: " + gatewayCode));

        return edgeOfflineSyncBatchRepository.findByGatewayIdOrderByCreatedAtDesc(gateway.getId()).stream()
                .map(b -> toResultDto(b, 0, 0))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EdgeSyncBatchResultDto> getRecentBatches() {
        return edgeOfflineSyncBatchRepository.findTop50ByOrderByCreatedAtDesc().stream()
                .map(b -> toResultDto(b, 0, 0))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EdgeTransactionLogDto> getTransactionsForBatch(UUID batchId) {
        return edgeOfflineTransactionLogRepository.findByBatchIdOrderBySequenceIdAsc(batchId).stream()
                .map(this::toTransactionLogDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EdgeTransactionLogDto> getRecentTransactions() {
        return edgeOfflineTransactionLogRepository.findTop100ByOrderBySyncedAtDesc().stream()
                .map(this::toTransactionLogDto)
                .collect(Collectors.toList());
    }

    private EdgeSyncBatchResultDto toResultDto(EdgeOfflineSyncBatch batch, int duplicateIgnored, int conflictResolved) {
        EdgeSyncBatchResultDto dto = new EdgeSyncBatchResultDto();
        dto.setId(batch.getId());
        dto.setBatchId(batch.getBatchId());
        if (batch.getGateway() != null) {
            dto.setGatewayCode(batch.getGateway().getGatewayCode());
        }
        dto.setSyncStatus(batch.getSyncStatus());
        dto.setTotalRecords(batch.getTotalRecords());
        dto.setProcessedRecords(batch.getProcessedRecords());
        dto.setFailedRecords(batch.getFailedRecords());
        dto.setDuplicateIgnoredRecords(duplicateIgnored);
        dto.setConflictResolvedRecords(conflictResolved);
        dto.setReconciliationNotes(batch.getReconciliationNotes());
        dto.setReconciledAt(batch.getReconciledAt());
        return dto;
    }

    private EdgeTransactionLogDto toTransactionLogDto(EdgeOfflineTransactionLog logEntity) {
        EdgeTransactionLogDto dto = new EdgeTransactionLogDto();
        dto.setId(logEntity.getId());
        if (logEntity.getBatch() != null) {
            dto.setBatchId(logEntity.getBatch().getId());
            dto.setBatchCode(logEntity.getBatch().getBatchId());
        }
        if (logEntity.getGateway() != null) {
            dto.setGatewayCode(logEntity.getGateway().getGatewayCode());
        }
        dto.setSequenceId(logEntity.getSequenceId());
        dto.setIdempotencyKey(logEntity.getIdempotencyKey());
        dto.setTransactionType(logEntity.getTransactionType());
        dto.setEntityType(logEntity.getEntityType());
        dto.setEntityId(logEntity.getEntityId());
        dto.setPayloadJson(logEntity.getPayloadJson());
        dto.setVectorClockVersion(logEntity.getVectorClockVersion());
        dto.setRecordedAt(logEntity.getRecordedAt());
        dto.setSyncedAt(logEntity.getSyncedAt());
        dto.setExecutionStatus(logEntity.getExecutionStatus());
        dto.setConflictResolutionNote(logEntity.getConflictResolutionNote());
        return dto;
    }
}
