package com.factoryos.modules.barcode.application;

import com.factoryos.modules.auth.repository.UserRepository;
import com.factoryos.modules.barcode.domain.BarcodeScanLog;
import com.factoryos.modules.barcode.domain.BomItem;
import com.factoryos.modules.barcode.domain.MaterialLot;
import com.factoryos.modules.barcode.dto.BarcodeScanRequest;
import com.factoryos.modules.barcode.dto.BarcodeScanResponse;
import com.factoryos.modules.barcode.repository.BarcodeScanLogRepository;
import com.factoryos.modules.barcode.repository.BomItemRepository;
import com.factoryos.modules.barcode.repository.MaterialLotRepository;
import com.factoryos.modules.machine.domain.Machine;
import com.factoryos.modules.machine.domain.MachineStatus;
import com.factoryos.modules.machine.repository.MachineRepository;
import com.factoryos.modules.production.domain.ProductionOrder;
import com.factoryos.modules.production.domain.ProductionOrderStatus;
import com.factoryos.modules.production.repository.ProductionOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class BarcodeScanningPerformanceTest {

    @Mock
    private MaterialLotRepository materialLotRepository;

    @Mock
    private BomItemRepository bomItemRepository;

    @Mock
    private BarcodeScanLogRepository barcodeScanLogRepository;

    @Mock
    private ProductionOrderRepository productionOrderRepository;

    @Mock
    private MachineRepository machineRepository;

    @Mock
    private UserRepository userRepository;

    private BarcodeScanningService barcodeScanningService;
    private UUID machineId;

    @BeforeEach
    void setUp() {
        barcodeScanningService = new BarcodeScanningService(
                materialLotRepository,
                bomItemRepository,
                barcodeScanLogRepository,
                productionOrderRepository,
                machineRepository,
                userRepository
        );

        machineId = UUID.randomUUID();
        Machine machine = new Machine();
        machine.setId(machineId);
        machine.setSerialNumber("CNC-01");
        machine.setName("CNC Milling Center");
        machine.setLocation("BAY-1");
        machine.setStatus(MachineStatus.RUNNING);

        ProductionOrder order = new ProductionOrder();
        order.setId(UUID.randomUUID());
        order.setOrderNumber("ORD-PERF-001");
        order.setProductCode("BRACKET-01");
        order.setStatus(ProductionOrderStatus.IN_PROGRESS);

        MaterialLot lot = new MaterialLot("LOT-ALU-6061-PERF", "RAW-ALU-6061", "Aluminum 6061 Billet",
                BigDecimal.valueOf(1000.0), "KG", "AVAILABLE", Instant.now().plus(180, ChronoUnit.DAYS), "Alcoa");

        BomItem bomItem = new BomItem("BRACKET-01", "RAW-ALU-6061", "Aluminum 6061 Billet", BigDecimal.valueOf(1.25), "KG");

        when(materialLotRepository.findByLotNumberIgnoreCase("LOT-ALU-6061-PERF")).thenReturn(Optional.of(lot));
        when(productionOrderRepository.findByMachineIdAndStatusAndIsDeletedFalse(machineId, ProductionOrderStatus.IN_PROGRESS))
                .thenReturn(List.of(order));
        when(bomItemRepository.findByProductCodeIgnoreCase("BRACKET-01")).thenReturn(List.of(bomItem));
        when(barcodeScanLogRepository.save(any(BarcodeScanLog.class))).thenAnswer(inv -> {
            BarcodeScanLog log = inv.getArgument(0);
            log.setId(UUID.randomUUID());
            return log;
        });
    }

    @Test
    void testBarcodeScanLatencyUnder300ms() {
        BarcodeScanRequest request = new BarcodeScanRequest("LOT:LOT-ALU-6061-PERF", "DATA_MATRIX", "HARDWARE_WEDGE", machineId, null);

        // Warm up
        for (int i = 0; i < 20; i++) {
            barcodeScanningService.processScan(request, UUID.randomUUID(), "Operator");
        }

        // Benchmark 100 consecutive scans
        long totalDurationMs = 0;
        int iterations = 100;

        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            BarcodeScanResponse response = barcodeScanningService.processScan(request, UUID.randomUUID(), "Operator");
            long durationMs = (System.nanoTime() - start) / 1_000_000;
            totalDurationMs += durationMs;

            assertTrue(durationMs < 300, "Individual scan latency exceeded 300ms SLA: " + durationMs + "ms");
        }

        double averageLatencyMs = (double) totalDurationMs / iterations;
        System.out.printf("[BENCHMARK] Average barcode scan latency across %d iterations: %.3f ms (SLA Target: <300ms)%n",
                iterations, averageLatencyMs);

        assertTrue(averageLatencyMs < 50.0, "Average latency must be well under 50ms (got: " + averageLatencyMs + "ms)");
    }
}
