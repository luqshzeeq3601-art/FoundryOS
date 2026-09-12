package com.factoryos.modules.edge.application;

import com.factoryos.modules.audit.application.AuditRecordingService;
import com.factoryos.modules.barcode.repository.BarcodeScanLogRepository;
import com.factoryos.modules.downtime.domain.DowntimeEvent;
import com.factoryos.modules.downtime.domain.DowntimeReasonCode;
import com.factoryos.modules.downtime.repository.DowntimeEventRepository;
import com.factoryos.modules.edge.domain.*;
import com.factoryos.modules.edge.dto.*;
import com.factoryos.modules.edge.repository.EdgeGatewayRepository;
import com.factoryos.modules.edge.repository.EdgeOfflineSyncBatchRepository;
import com.factoryos.modules.edge.repository.EdgeOfflineTransactionLogRepository;
import com.factoryos.modules.edge.service.EdgeStoreAndForwardSyncService;
import com.factoryos.modules.machine.domain.Machine;
import com.factoryos.modules.machine.domain.MachineStatus;
import com.factoryos.modules.machine.repository.MachineRepository;
import com.factoryos.modules.production.domain.ProductionOrder;
import com.factoryos.modules.production.domain.ProductionOrderStatus;
import com.factoryos.modules.production.repository.ProductionOrderRepository;
import com.factoryos.modules.tenant.domain.Plant;
import com.factoryos.modules.tenant.repository.PlantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
class EdgeStoreAndForwardRehearsalTest {

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

    private Plant austinPlant;
    private Machine cncMachine;
    private ProductionOrder activeOrder;
    private EdgeGateway edgeGateway;

    // In-memory transaction idempotency ledger to simulate real DB behavior across replays
    private final Set<String> persistentIdempotencyLedger = new HashSet<>();
    private final List<EdgeOfflineTransactionLog> persistedLogs = new ArrayList<>();
    private final List<DowntimeEvent> persistedDowntimeEvents = new ArrayList<>();

    @BeforeEach
    void setUp() {
        persistentIdempotencyLedger.clear();
        persistedLogs.clear();
        persistedDowntimeEvents.clear();

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

        austinPlant = new Plant();
        austinPlant.setId(UUID.randomUUID());
        austinPlant.setName("Austin Gigafactory");

        cncMachine = new Machine();
        cncMachine.setId(UUID.randomUUID());
        cncMachine.setSerialNumber("CNC-AUSTIN-001");
        cncMachine.setPlant(austinPlant);
        cncMachine.setStatus(MachineStatus.RUNNING);

        activeOrder = new ProductionOrder();
        activeOrder.setId(UUID.randomUUID());
        activeOrder.setOrderNumber("PO-E5S3-REHEARSAL-01");
        activeOrder.setPlant(austinPlant);
        activeOrder.setMachine(cncMachine);
        activeOrder.setProductCode("CAST-TURBINE-AUSTIN");
        activeOrder.setPlannedQuantity(5000);
        activeOrder.setGoodQuantity(500); // Baseline online parts before disconnect
        activeOrder.setScrapQuantity(10);
        activeOrder.setStatus(ProductionOrderStatus.IN_PROGRESS);

        edgeGateway = new EdgeGateway();
        edgeGateway.setId(UUID.randomUUID());
        edgeGateway.setGatewayCode("EDGE-GW-AUSTIN-01");
        edgeGateway.setName("Austin High-Speed Edge Hub");
        edgeGateway.setPlant(austinPlant);
        edgeGateway.setStatus("ONLINE");
        edgeGateway.setLastSyncSequenceId(5000L);
    }

    @Nested
    @DisplayName("Gate 1: 4-Hour WAN Disconnect & Zero-Part-Loss Sync Verification")
    class DisconnectRehearsalVerification {

        @Test
        @DisplayName("Rehearsal: 1,000+ parts buffered offline over 4 hours reconcile with zero loss")
        void testSimulate4HourDisconnectAndReconcile() {
            // Setup dynamic mock behavior simulating persistent database constraints
            when(edgeGatewayRepository.findByGatewayCodeAndIsDeletedFalse("EDGE-GW-AUSTIN-01"))
                    .thenReturn(Optional.of(edgeGateway));
            when(plantRepository.findByIdAndIsDeletedFalse(any())).thenReturn(Optional.of(austinPlant));
            when(productionOrderRepository.findByIdAndIsDeletedFalse(activeOrder.getId()))
                    .thenReturn(Optional.of(activeOrder));
            when(machineRepository.findByIdAndIsDeletedFalse(cncMachine.getId()))
                    .thenReturn(Optional.of(cncMachine));

            when(edgeOfflineSyncBatchRepository.save(any(EdgeOfflineSyncBatch.class))).thenAnswer(i -> i.getArgument(0));
            when(edgeOfflineTransactionLogRepository.existsByIdempotencyKey(anyString()))
                    .thenAnswer(invocation -> persistentIdempotencyLedger.contains(invocation.getArgument(0)));

            when(edgeOfflineTransactionLogRepository.save(any(EdgeOfflineTransactionLog.class)))
                    .thenAnswer(invocation -> {
                        EdgeOfflineTransactionLog log = invocation.getArgument(0);
                        persistentIdempotencyLedger.add(log.getIdempotencyKey());
                        persistedLogs.add(log);
                        return log;
                    });

            when(downtimeEventRepository.save(any(DowntimeEvent.class)))
                    .thenAnswer(invocation -> {
                        DowntimeEvent dt = invocation.getArgument(0);
                        persistedDowntimeEvents.add(dt);
                        return dt;
                    });

            // 1. Arrange: 4-hour offline period with 1,200 good parts, 15 scrap, 30 barcode scans, 15m downtime
            int expectedOfflineGood = 1200;
            int expectedOfflineScrap = 15;
            int baselineGood = activeOrder.getGoodQuantity();
            int baselineScrap = activeOrder.getScrapQuantity();

            SimulateDisconnectRequestDto simulationRequest = new SimulateDisconnectRequestDto();
            simulationRequest.setGatewayCode("EDGE-GW-AUSTIN-01");
            simulationRequest.setOrderId(activeOrder.getId());
            simulationRequest.setMachineId(cncMachine.getId());
            simulationRequest.setDisconnectDurationHours(4.0);
            simulationRequest.setProducedPartsGood(expectedOfflineGood);
            simulationRequest.setProducedPartsScrap(expectedOfflineScrap);
            simulationRequest.setBarcodeScans(30);
            simulationRequest.setDowntimeDurationMinutes(15);
            simulationRequest.setDowntimeReason("Spindle Overheat Micro-Pause");

            // 2. Act: Trigger 4-hour simulated disconnect and store-and-forward batch replay
            EdgeSyncBatchResultDto result = syncService.simulate4HourDisconnect(simulationRequest);

            // 3. Assert Sync Result Status
            assertNotNull(result);
            assertEquals(SyncStatus.RECONCILED, result.getSyncStatus());
            assertTrue(result.getProcessedRecords() >= 60, "Must have processed all buffered production + scan + downtime records");
            assertEquals(0, result.getFailedRecords());
            assertEquals(0, result.getDuplicateIgnoredRecords());

            // 4. Verify Zero Loss Invariant: Exact Part Counts
            int expectedFinalGood = baselineGood + expectedOfflineGood; // 500 + 1200 = 1700
            int expectedFinalScrap = baselineScrap + expectedOfflineScrap; // 10 + 15 = 25
            assertEquals(expectedFinalGood, activeOrder.getGoodQuantity(), "Part count must reconcile with 0 loss");
            assertEquals(expectedFinalScrap, activeOrder.getScrapQuantity(), "Scrap count must reconcile with 0 loss");

            // 5. Verify Downtime Preservation: Exact duration logged
            assertFalse(persistedDowntimeEvents.isEmpty(), "Offline downtime event must be persisted");
            DowntimeEvent downtime = persistedDowntimeEvents.get(0);
            assertEquals(cncMachine, downtime.getMachine());
            assertEquals(DowntimeReasonCode.TOOLING_JAM, downtime.getReasonCode());
            long recordedDurationMinutes = ChronoUnit.MINUTES.between(downtime.getStartTime(), downtime.getEndTime());
            assertEquals(15, recordedDurationMinutes, "Downtime duration must be preserved exactly as 15 minutes");

            // 6. Verify Gateway Monotonic Sequence Update
            assertTrue(edgeGateway.getLastSyncSequenceId() > 5000L, "Gateway sync sequence must advance monotonically");
            assertEquals("ONLINE", edgeGateway.getStatus());
        }

        @Test
        @DisplayName("Replay Idempotency: Duplicate sync replay yields 0 duplicate counts and no state drift")
        void testDuplicateBatchReplayPreventsDoubleCounting() {
            when(edgeGatewayRepository.findByGatewayCodeAndIsDeletedFalse("EDGE-GW-AUSTIN-01"))
                    .thenReturn(Optional.of(edgeGateway));
            when(plantRepository.findByIdAndIsDeletedFalse(any())).thenReturn(Optional.of(austinPlant));
            when(productionOrderRepository.findByIdAndIsDeletedFalse(activeOrder.getId()))
                    .thenReturn(Optional.of(activeOrder));
            when(edgeOfflineSyncBatchRepository.save(any(EdgeOfflineSyncBatch.class))).thenAnswer(i -> i.getArgument(0));

            when(edgeOfflineTransactionLogRepository.existsByIdempotencyKey(anyString()))
                    .thenAnswer(invocation -> persistentIdempotencyLedger.contains(invocation.getArgument(0)));
            when(edgeOfflineTransactionLogRepository.save(any(EdgeOfflineTransactionLog.class)))
                    .thenAnswer(invocation -> {
                        EdgeOfflineTransactionLog log = invocation.getArgument(0);
                        persistentIdempotencyLedger.add(log.getIdempotencyKey());
                        return log;
                    });

            // 1. Build a synthetic batch of 100 parts
            EdgeSyncBatchRequestDto batch = new EdgeSyncBatchRequestDto();
            batch.setGatewayCode("EDGE-GW-AUSTIN-01");
            batch.setBatchId("BATCH-IDEMPOTENCY-TEST-1");
            batch.setPlantId(austinPlant.getId());
            batch.setSequenceStart(5001L);
            batch.setSequenceEnd(5005L);
            batch.setDisconnectedAt(Instant.now().minus(4, ChronoUnit.HOURS));
            batch.setReconnectedAt(Instant.now());

            List<EdgeTransactionRecordDto> txs = new ArrayList<>();
            for (int i = 1; i <= 5; i++) {
                txs.add(new EdgeTransactionRecordDto(5000L + i, "IDEM-KEY-" + i,
                        TransactionType.PRODUCTION_OUTPUT, "ProductionOrder", activeOrder.getId(),
                        "{\"deltaGood\": 20, \"deltaScrap\": 0}", 1L, Instant.now().minus(3, ChronoUnit.HOURS)));
            }
            batch.setTransactions(txs);

            // 2. First execution: All 5 transactions are processed
            EdgeSyncBatchResultDto firstPass = syncService.processSyncBatch(batch);
            assertEquals(5, firstPass.getProcessedRecords());
            assertEquals(0, firstPass.getDuplicateIgnoredRecords());
            assertEquals(600, activeOrder.getGoodQuantity()); // 500 + 100 = 600

            // 3. Second execution (Network duplicate replay of identical batch payload)
            when(edgeOfflineSyncBatchRepository.findByBatchId("BATCH-IDEMPOTENCY-TEST-1")).thenReturn(Optional.empty());

            EdgeSyncBatchResultDto secondPass = syncService.processSyncBatch(batch);

            // 4. Assert: All 5 transactions were recognized as duplicates and ignored
            assertEquals(0, secondPass.getProcessedRecords());
            assertEquals(5, secondPass.getDuplicateIgnoredRecords());

            // 5. Invariant check: Part count did NOT double count! Remained exactly 600
            assertEquals(600, activeOrder.getGoodQuantity(), "Part count must NOT double count on replayed batch");
        }
    }
}
