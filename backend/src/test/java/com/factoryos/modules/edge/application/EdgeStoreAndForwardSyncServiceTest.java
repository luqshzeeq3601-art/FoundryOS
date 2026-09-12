package com.factoryos.modules.edge.application;

import com.factoryos.modules.audit.application.AuditRecordingService;
import com.factoryos.modules.barcode.repository.BarcodeScanLogRepository;
import com.factoryos.modules.downtime.repository.DowntimeEventRepository;
import com.factoryos.modules.edge.domain.*;
import com.factoryos.modules.edge.dto.*;
import com.factoryos.modules.edge.repository.EdgeGatewayRepository;
import com.factoryos.modules.edge.repository.EdgeOfflineSyncBatchRepository;
import com.factoryos.modules.edge.repository.EdgeOfflineTransactionLogRepository;
import com.factoryos.modules.edge.service.EdgeStoreAndForwardSyncService;
import com.factoryos.modules.machine.domain.Machine;
import com.factoryos.modules.machine.repository.MachineRepository;
import com.factoryos.modules.production.domain.ProductionOrder;
import com.factoryos.modules.production.repository.ProductionOrderRepository;
import com.factoryos.modules.tenant.domain.Plant;
import com.factoryos.modules.tenant.repository.PlantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EdgeStoreAndForwardSyncServiceTest {

    @Mock
    private EdgeGatewayRepository edgeGatewayRepository;

    @Mock
    private EdgeOfflineSyncBatchRepository edgeOfflineSyncBatchRepository;

    @Mock
    private EdgeOfflineTransactionLogRepository edgeOfflineTransactionLogRepository;

    @Mock
    private ProductionOrderRepository productionOrderRepository;

    @Mock
    private BarcodeScanLogRepository barcodeScanLogRepository;

    @Mock
    private DowntimeEventRepository downtimeEventRepository;

    @Mock
    private MachineRepository machineRepository;

    @Mock
    private PlantRepository plantRepository;

    @Mock
    private AuditRecordingService auditRecordingService;

    private EdgeStoreAndForwardSyncService syncService;

    private Plant testPlant;
    private EdgeGateway testGateway;
    private ProductionOrder testOrder;

    @BeforeEach
    void setUp() {
        syncService = new EdgeStoreAndForwardSyncService(
                edgeGatewayRepository,
                edgeOfflineSyncBatchRepository,
                edgeOfflineTransactionLogRepository,
                productionOrderRepository,
                barcodeScanLogRepository,
                downtimeEventRepository,
                machineRepository,
                plantRepository,
                auditRecordingService
        );

        testPlant = new Plant();
        testPlant.setId(UUID.randomUUID());
        testPlant.setName("Austin Gigafactory");

        testGateway = new EdgeGateway();
        testGateway.setId(UUID.randomUUID());
        testGateway.setGatewayCode("EDGE-GW-AUSTIN-01");
        testGateway.setName("Austin Primary Gateway");
        testGateway.setPlant(testPlant);
        testGateway.setStatus("ONLINE");
        testGateway.setLastSyncSequenceId(100L);

        testOrder = new ProductionOrder();
        testOrder.setId(UUID.randomUUID());
        testOrder.setOrderNumber("PO-2026-001");
        testOrder.setPlant(testPlant);
        testOrder.setPlannedQuantity(1000);
        testOrder.setGoodQuantity(200);
        testOrder.setScrapQuantity(5);
    }

    @Test
    @DisplayName("Batch Sync: Successfully reconciles production outputs with additive delta")
    void testProcessSyncBatchAdditiveProduction() {
        when(edgeGatewayRepository.findByGatewayCodeAndIsDeletedFalse("EDGE-GW-AUSTIN-01"))
                .thenReturn(Optional.of(testGateway));
        when(plantRepository.findByIdAndIsDeletedFalse(testPlant.getId()))
                .thenReturn(Optional.of(testPlant));
        when(edgeOfflineSyncBatchRepository.findByBatchId(any())).thenReturn(Optional.empty());
        when(edgeOfflineSyncBatchRepository.save(any(EdgeOfflineSyncBatch.class))).thenAnswer(i -> i.getArgument(0));
        when(edgeOfflineTransactionLogRepository.existsByIdempotencyKey(any())).thenReturn(false);
        when(productionOrderRepository.findByIdAndIsDeletedFalse(testOrder.getId())).thenReturn(Optional.of(testOrder));

        EdgeSyncBatchRequestDto request = new EdgeSyncBatchRequestDto();
        request.setGatewayCode("EDGE-GW-AUSTIN-01");
        request.setBatchId("BATCH-TEST-001");
        request.setPlantId(testPlant.getId());
        request.setSequenceStart(101L);
        request.setSequenceEnd(103L);
        request.setDisconnectedAt(Instant.now().minus(2, ChronoUnit.HOURS));
        request.setReconnectedAt(Instant.now());

        EdgeTransactionRecordDto tx1 = new EdgeTransactionRecordDto(101L, "KEY-101",
                TransactionType.PRODUCTION_OUTPUT, "ProductionOrder", testOrder.getId(),
                "{\"deltaGood\": 50, \"deltaScrap\": 2}", 1L, Instant.now().minus(90, ChronoUnit.MINUTES));

        EdgeTransactionRecordDto tx2 = new EdgeTransactionRecordDto(102L, "KEY-102",
                TransactionType.PRODUCTION_OUTPUT, "ProductionOrder", testOrder.getId(),
                "{\"deltaGood\": 30, \"deltaScrap\": 1}", 1L, Instant.now().minus(60, ChronoUnit.MINUTES));

        request.setTransactions(List.of(tx1, tx2));

        EdgeSyncBatchResultDto result = syncService.processSyncBatch(request);

        assertEquals(SyncStatus.RECONCILED, result.getSyncStatus());
        assertEquals(2, result.getProcessedRecords());
        assertEquals(0, result.getFailedRecords());
        assertEquals(0, result.getDuplicateIgnoredRecords());

        // Verify additive output updates: 200 + 50 + 30 = 280 good, 5 + 2 + 1 = 8 scrap
        assertEquals(280, testOrder.getGoodQuantity());
        assertEquals(8, testOrder.getScrapQuantity());
        verify(productionOrderRepository, times(2)).save(testOrder);
        verify(edgeOfflineTransactionLogRepository, times(2)).save(any(EdgeOfflineTransactionLog.class));
    }

    @Test
    @DisplayName("Idempotency: Ignores duplicate transactions without double-counting")
    void testProcessSyncBatchIgnoresDuplicates() {
        when(edgeGatewayRepository.findByGatewayCodeAndIsDeletedFalse("EDGE-GW-AUSTIN-01"))
                .thenReturn(Optional.of(testGateway));
        when(plantRepository.findByIdAndIsDeletedFalse(testPlant.getId()))
                .thenReturn(Optional.of(testPlant));
        when(edgeOfflineSyncBatchRepository.findByBatchId("BATCH-TEST-DUP")).thenReturn(Optional.empty());
        when(edgeOfflineSyncBatchRepository.save(any(EdgeOfflineSyncBatch.class))).thenAnswer(i -> i.getArgument(0));

        // Simulate KEY-DUP-1 already exists in database
        when(edgeOfflineTransactionLogRepository.existsByIdempotencyKey("KEY-DUP-1")).thenReturn(true);
        when(edgeOfflineTransactionLogRepository.existsByIdempotencyKey("KEY-NEW-2")).thenReturn(false);
        when(productionOrderRepository.findByIdAndIsDeletedFalse(testOrder.getId())).thenReturn(Optional.of(testOrder));

        EdgeSyncBatchRequestDto request = new EdgeSyncBatchRequestDto();
        request.setGatewayCode("EDGE-GW-AUSTIN-01");
        request.setBatchId("BATCH-TEST-DUP");
        request.setPlantId(testPlant.getId());
        request.setSequenceStart(101L);
        request.setSequenceEnd(102L);

        EdgeTransactionRecordDto tx1 = new EdgeTransactionRecordDto(101L, "KEY-DUP-1",
                TransactionType.PRODUCTION_OUTPUT, "ProductionOrder", testOrder.getId(),
                "{\"deltaGood\": 50, \"deltaScrap\": 0}", 1L, Instant.now());

        EdgeTransactionRecordDto tx2 = new EdgeTransactionRecordDto(102L, "KEY-NEW-2",
                TransactionType.PRODUCTION_OUTPUT, "ProductionOrder", testOrder.getId(),
                "{\"deltaGood\": 20, \"deltaScrap\": 0}", 1L, Instant.now());

        request.setTransactions(List.of(tx1, tx2));

        EdgeSyncBatchResultDto result = syncService.processSyncBatch(request);

        assertEquals(SyncStatus.RECONCILED, result.getSyncStatus());
        assertEquals(1, result.getProcessedRecords());
        assertEquals(1, result.getDuplicateIgnoredRecords());

        // Good quantity incremented ONLY once by 20 (200 + 20 = 220), duplicate 50 was skipped!
        assertEquals(220, testOrder.getGoodQuantity());
        verify(productionOrderRepository, times(1)).save(testOrder);
    }

    @Test
    @DisplayName("Downtime Sync: Preserves exact downtime interval and duration")
    void testProcessSyncBatchPreservesDowntime() {
        Machine testMachine = new Machine();
        testMachine.setId(UUID.randomUUID());
        testMachine.setSerialNumber("CNC-AUSTIN-01");
        testMachine.setPlant(testPlant);

        when(edgeGatewayRepository.findByGatewayCodeAndIsDeletedFalse("EDGE-GW-AUSTIN-01"))
                .thenReturn(Optional.of(testGateway));
        when(plantRepository.findByIdAndIsDeletedFalse(testPlant.getId()))
                .thenReturn(Optional.of(testPlant));
        when(edgeOfflineSyncBatchRepository.findByBatchId(any())).thenReturn(Optional.empty());
        when(edgeOfflineSyncBatchRepository.save(any(EdgeOfflineSyncBatch.class))).thenAnswer(i -> i.getArgument(0));
        when(edgeOfflineTransactionLogRepository.existsByIdempotencyKey(any())).thenReturn(false);
        when(machineRepository.findByIdAndIsDeletedFalse(testMachine.getId())).thenReturn(Optional.of(testMachine));

        Instant dtStart = Instant.now().minus(3, ChronoUnit.HOURS);
        Instant dtEnd = dtStart.plus(25, ChronoUnit.MINUTES);

        EdgeSyncBatchRequestDto request = new EdgeSyncBatchRequestDto();
        request.setGatewayCode("EDGE-GW-AUSTIN-01");
        request.setBatchId("BATCH-DT-001");
        request.setPlantId(testPlant.getId());

        EdgeTransactionRecordDto dtTx = new EdgeTransactionRecordDto(101L, "KEY-DT-101",
                TransactionType.DOWNTIME_EVENT, "Machine", testMachine.getId(),
                String.format("{\"reasonCode\": \"TOOLING_JAM\", \"startTime\": \"%s\", \"endTime\": \"%s\", \"durationMinutes\": 25}",
                        dtStart.toString(), dtEnd.toString()),
                1L, dtStart);

        request.setTransactions(List.of(dtTx));

        EdgeSyncBatchResultDto result = syncService.processSyncBatch(request);

        assertEquals(SyncStatus.RECONCILED, result.getSyncStatus());
        assertEquals(1, result.getProcessedRecords());
        verify(downtimeEventRepository, times(1)).save(any());
    }
}
