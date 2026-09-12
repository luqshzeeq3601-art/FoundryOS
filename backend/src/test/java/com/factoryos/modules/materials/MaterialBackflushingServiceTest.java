package com.factoryos.modules.materials;

import com.factoryos.modules.audit.application.AuditRecordingService;
import com.factoryos.modules.auth.domain.User;
import com.factoryos.modules.barcode.domain.BomItem;
import com.factoryos.modules.barcode.repository.BomItemRepository;
import com.factoryos.modules.barcode.repository.MaterialLotRepository;
import com.factoryos.modules.materials.application.MaterialBackflushingService;
import com.factoryos.modules.materials.domain.Material;
import com.factoryos.modules.materials.domain.MaterialConsumptionRecord;
import com.factoryos.modules.materials.dto.BomExplosionDto;
import com.factoryos.modules.materials.dto.MaterialConsumptionRecordDto;
import com.factoryos.modules.materials.dto.RecordProductionOutputRequest;
import com.factoryos.modules.materials.repository.MaterialConsumptionRecordRepository;
import com.factoryos.modules.materials.repository.MaterialRepository;
import com.factoryos.modules.production.domain.ProductionOrder;
import com.factoryos.modules.production.domain.ProductionOrderStatus;
import com.factoryos.modules.production.repository.ProductionOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaterialBackflushingServiceTest {

    @Mock
    private MaterialRepository materialRepository;
    @Mock
    private MaterialConsumptionRecordRepository consumptionRecordRepository;
    @Mock
    private BomItemRepository bomItemRepository;
    @Mock
    private ProductionOrderRepository productionOrderRepository;
    @Mock
    private MaterialLotRepository materialLotRepository;
    @Mock
    private AuditRecordingService auditRecordingService;

    private MaterialBackflushingService service;

    @BeforeEach
    void setUp() {
        service = new MaterialBackflushingService(
                materialRepository,
                consumptionRecordRepository,
                bomItemRepository,
                productionOrderRepository,
                materialLotRepository,
                auditRecordingService
        );
    }

    @Test
    @DisplayName("Should backflush material inventory based on BOM ratio upon good piece production")
    void testRecordOutputDecrementsInventory() {
        UUID orderId = UUID.randomUUID();
        ProductionOrder order = new ProductionOrder();
        order.setId(orderId);
        order.setOrderNumber("PO-2026-001");
        order.setProductCode("SPINDLE-ROT-V2");
        order.setStatus(ProductionOrderStatus.IN_PROGRESS);
        order.setGoodQuantity(10);
        order.setScrapQuantity(0);

        BomItem bom1 = new BomItem("SPINDLE-ROT-V2", "RAW-ALU-6061", "Aluminum 6061 Billet", BigDecimal.valueOf(2.5), "KG");
        Material mat1 = new Material("RAW-ALU-6061", "Aluminum 6061 Billet", "RAW_MATERIAL", "KG", BigDecimal.valueOf(500.0), BigDecimal.valueOf(15.0), "CC-SCRAP-MACHINING");

        when(productionOrderRepository.findByIdAndIsDeletedFalse(orderId)).thenReturn(Optional.of(order));
        when(bomItemRepository.findByProductCodeIgnoreCase("SPINDLE-ROT-V2")).thenReturn(List.of(bom1));
        when(materialRepository.findByMaterialCodeIgnoreCaseAndIsDeletedFalse("RAW-ALU-6061")).thenReturn(Optional.of(mat1));
        when(consumptionRecordRepository.save(any(MaterialConsumptionRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        RecordProductionOutputRequest req = new RecordProductionOutputRequest();
        req.setIncrementalGoodQuantity(20);
        req.setIncrementalScrapQuantity(0);

        List<MaterialConsumptionRecordDto> records = service.recordProductionOutputAndBackflush(orderId, req, null);

        assertThat(records).hasSize(1);
        MaterialConsumptionRecordDto rec = records.get(0);
        // 20 pieces * 2.5 KG = 50.0 KG theoretical
        assertThat(rec.getTheoreticalQuantity()).isEqualByComparingTo(BigDecimal.valueOf(50.0));
        assertThat(rec.getActualQuantity()).isEqualByComparingTo(BigDecimal.valueOf(50.0));
        assertThat(rec.getVariancePercentage()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(rec.isVarianceAlertTriggered()).isFalse();

        // Stock was 500.0 - 50.0 = 450.0
        assertThat(mat1.getCurrentStock()).isEqualByComparingTo(BigDecimal.valueOf(450.0));
        assertThat(order.getGoodQuantity()).isEqualTo(30);
    }

    @Test
    @DisplayName("Should trigger discrepancy warning when actual consumption exceeds theoretical BOM by > 5%")
    void testVarianceAlertTriggeredWhenDeviationExceedsFivePercent() {
        UUID orderId = UUID.randomUUID();
        ProductionOrder order = new ProductionOrder();
        order.setId(orderId);
        order.setOrderNumber("PO-2026-002");
        order.setProductCode("GEAR-02");
        order.setStatus(ProductionOrderStatus.IN_PROGRESS);

        BomItem bom = new BomItem("GEAR-02", "RAW-STL-4140", "Steel 4140", BigDecimal.valueOf(2.0), "KG");
        Material mat = new Material("RAW-STL-4140", "Steel 4140", "RAW_MATERIAL", "KG", BigDecimal.valueOf(300.0), BigDecimal.valueOf(20.0), "CC-SCRAP-HEAT-TREAT");

        when(productionOrderRepository.findByIdAndIsDeletedFalse(orderId)).thenReturn(Optional.of(order));
        when(bomItemRepository.findByProductCodeIgnoreCase("GEAR-02")).thenReturn(List.of(bom));
        when(materialRepository.findByMaterialCodeIgnoreCaseAndIsDeletedFalse("RAW-STL-4140")).thenReturn(Optional.of(mat));
        when(consumptionRecordRepository.save(any(MaterialConsumptionRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        RecordProductionOutputRequest req = new RecordProductionOutputRequest();
        req.setIncrementalGoodQuantity(10); // 10 * 2.0 = 20.0 KG theoretical
        req.setIncrementalScrapQuantity(0);
        // Actual used: 22.5 KG (deviation: +12.5% > 5%)
        req.setActualQuantities(Map.of("RAW-STL-4140", BigDecimal.valueOf(22.5)));

        List<MaterialConsumptionRecordDto> records = service.recordProductionOutputAndBackflush(orderId, req, null);

        assertThat(records).hasSize(1);
        MaterialConsumptionRecordDto rec = records.get(0);
        assertThat(rec.getTheoreticalQuantity()).isEqualByComparingTo(BigDecimal.valueOf(20.0));
        assertThat(rec.getActualQuantity()).isEqualByComparingTo(BigDecimal.valueOf(22.5));
        assertThat(rec.getVariancePercentage()).isEqualByComparingTo(BigDecimal.valueOf(12.5));
        assertThat(rec.isVarianceAlertTriggered()).isTrue();

        verify(auditRecordingService, times(1)).record(
                any(),
                eq("MATERIAL_CONSUMPTION_VARIANCE_ALERT"),
                eq("MaterialConsumptionRecord"),
                any(),
                any(),
                any()
        );
    }

    @Test
    @DisplayName("Should generate BOM explosion with stock sufficiency and cost breakdown")
    void testBomExplosionCalculation() {
        BomItem bom1 = new BomItem("BRACKET-01", "RAW-ALU-6061", "Aluminum 6061 Billet", BigDecimal.valueOf(1.25), "KG");
        Material mat1 = new Material("RAW-ALU-6061", "Aluminum 6061 Billet", "RAW_MATERIAL", "KG", BigDecimal.valueOf(500.0), BigDecimal.valueOf(14.0), "CC-SCRAP-MACHINING");

        when(bomItemRepository.findByProductCodeIgnoreCase("BRACKET-01")).thenReturn(List.of(bom1));
        when(materialRepository.findByMaterialCodeIgnoreCaseAndIsDeletedFalse("RAW-ALU-6061")).thenReturn(Optional.of(mat1));

        BomExplosionDto explosion = service.getBomExplosion("BRACKET-01", 100);

        assertThat(explosion.getProductCode()).isEqualTo("BRACKET-01");
        assertThat(explosion.getPlannedQuantity()).isEqualTo(100);
        assertThat(explosion.getComponents()).hasSize(1);

        BomExplosionDto.BomComponentItem item = explosion.getComponents().get(0);
        // 100 * 1.25 = 125.0 KG total required
        assertThat(item.getTotalRequiredQuantity()).isEqualByComparingTo(BigDecimal.valueOf(125.0));
        assertThat(item.isStockSufficient()).isTrue(); // 500 >= 125
        assertThat(item.getTotalCost()).isEqualByComparingTo(BigDecimal.valueOf(1750.0)); // 125 * 14 = 1750
        assertThat(explosion.isAllMaterialsInStock()).isTrue();
    }
}
