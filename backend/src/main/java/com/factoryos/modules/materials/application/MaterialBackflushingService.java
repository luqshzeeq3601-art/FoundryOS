package com.factoryos.modules.materials.application;

import com.factoryos.common.exception.AppException;
import com.factoryos.modules.audit.application.AuditRecordingService;
import com.factoryos.modules.auth.domain.User;
import com.factoryos.modules.barcode.domain.BomItem;
import com.factoryos.modules.barcode.domain.MaterialLot;
import com.factoryos.modules.barcode.repository.BomItemRepository;
import com.factoryos.modules.barcode.repository.MaterialLotRepository;
import com.factoryos.modules.materials.domain.Material;
import com.factoryos.modules.materials.domain.MaterialConsumptionRecord;
import com.factoryos.modules.materials.dto.*;
import com.factoryos.modules.materials.repository.MaterialConsumptionRecordRepository;
import com.factoryos.modules.materials.repository.MaterialRepository;
import com.factoryos.modules.production.domain.ProductionOrder;
import com.factoryos.modules.production.repository.ProductionOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

@Service
public class MaterialBackflushingService {

    private static final Logger log = LoggerFactory.getLogger(MaterialBackflushingService.class);

    private final MaterialRepository materialRepository;
    private final MaterialConsumptionRecordRepository consumptionRecordRepository;
    private final BomItemRepository bomItemRepository;
    private final ProductionOrderRepository productionOrderRepository;
    private final MaterialLotRepository materialLotRepository;
    private final AuditRecordingService auditRecordingService;

    public MaterialBackflushingService(
            MaterialRepository materialRepository,
            MaterialConsumptionRecordRepository consumptionRecordRepository,
            BomItemRepository bomItemRepository,
            ProductionOrderRepository productionOrderRepository,
            MaterialLotRepository materialLotRepository,
            AuditRecordingService auditRecordingService) {
        this.materialRepository = materialRepository;
        this.consumptionRecordRepository = consumptionRecordRepository;
        this.bomItemRepository = bomItemRepository;
        this.productionOrderRepository = productionOrderRepository;
        this.materialLotRepository = materialLotRepository;
        this.auditRecordingService = auditRecordingService;
    }

    @Transactional(readOnly = true)
    public List<MaterialDto> getAllMaterials() {
        return materialRepository.findByIsDeletedFalseOrderByMaterialCodeAsc().stream()
                .map(MaterialDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public BomExplosionDto getBomExplosion(String productCode, int plannedQty) {
        BomExplosionDto explosion = new BomExplosionDto(productCode, plannedQty);
        List<BomItem> items = bomItemRepository.findByProductCodeIgnoreCase(productCode);
        if (items.isEmpty()) {
            items = bomItemRepository.findByProductCode(productCode);
        }

        boolean allInStock = true;
        BigDecimal totalCost = BigDecimal.ZERO;
        List<BomExplosionDto.BomComponentItem> components = new ArrayList<>();

        for (BomItem item : items) {
            BomExplosionDto.BomComponentItem c = new BomExplosionDto.BomComponentItem();
            c.setMaterialCode(item.getMaterialCode());
            c.setMaterialName(item.getMaterialName());
            c.setRequiredPerUnit(item.getRequiredQuantityPerUnit());
            c.setUom(item.getUom());

            BigDecimal totalReq = item.getRequiredQuantityPerUnit().multiply(BigDecimal.valueOf(Math.max(1, plannedQty)));
            c.setTotalRequiredQuantity(totalReq);

            Optional<Material> matOpt = materialRepository.findByMaterialCodeIgnoreCaseAndIsDeletedFalse(item.getMaterialCode());
            if (matOpt.isPresent()) {
                Material mat = matOpt.get();
                c.setCurrentStock(mat.getCurrentStock());
                c.setScrapCostCenter(mat.getScrapCostCenter());
                c.setUnitCost(mat.getStandardCost());
                BigDecimal componentCost = mat.getStandardCost().multiply(totalReq);
                c.setTotalCost(componentCost);
                totalCost = totalCost.add(componentCost);

                boolean sufficient = mat.getCurrentStock().compareTo(totalReq) >= 0;
                c.setStockSufficient(sufficient);
                if (!sufficient) {
                    allInStock = false;
                }
            } else {
                c.setCurrentStock(BigDecimal.ZERO);
                c.setStockSufficient(false);
                c.setScrapCostCenter("CC-SCRAP-MACHINING");
                c.setUnitCost(BigDecimal.TEN);
                c.setTotalCost(BigDecimal.TEN.multiply(totalReq));
                allInStock = false;
            }
            components.add(c);
        }

        explosion.setComponents(components);
        explosion.setAllMaterialsInStock(allInStock);
        explosion.setTotalEstimatedMaterialCost(totalCost);
        return explosion;
    }

    @Transactional
    public List<MaterialConsumptionRecordDto> recordProductionOutputAndBackflush(UUID orderId, RecordProductionOutputRequest request, User actor) {
        ProductionOrder order = productionOrderRepository.findByIdAndIsDeletedFalse(orderId)
                .orElseThrow(() -> AppException.notFound("Production order not found with ID: " + orderId));

        int goodIncrement = request.getIncrementalGoodQuantity();
        int scrapIncrement = request.getIncrementalScrapQuantity();
        int totalProduced = goodIncrement + scrapIncrement;

        if (totalProduced <= 0) {
            throw AppException.badRequest("Total pieces produced (good + scrap) must be greater than zero.");
        }

        List<BomItem> bomItems = bomItemRepository.findByProductCodeIgnoreCase(order.getProductCode());
        if (bomItems.isEmpty()) {
            bomItems = bomItemRepository.findByProductCode(order.getProductCode());
        }

        List<MaterialConsumptionRecordDto> recordDtos = new ArrayList<>();

        for (BomItem bom : bomItems) {
            BigDecimal theoreticalQty = bom.getRequiredQuantityPerUnit().multiply(BigDecimal.valueOf(totalProduced));
            BigDecimal actualQty = theoreticalQty;

            if (request.getActualQuantities() != null && request.getActualQuantities().containsKey(bom.getMaterialCode())) {
                actualQty = request.getActualQuantities().get(bom.getMaterialCode());
            }

            BigDecimal variancePct = BigDecimal.ZERO;
            if (theoreticalQty.compareTo(BigDecimal.ZERO) > 0) {
                variancePct = actualQty.subtract(theoreticalQty)
                        .divide(theoreticalQty, 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);
            }

            // Invariant: Discrepancy warnings raised when actual consumption deviates > 5% from theoretical BOM
            boolean varianceAlert = variancePct.abs().compareTo(BigDecimal.valueOf(5.0)) > 0;

            // Decrement stock from Material master
            Optional<Material> matOpt = materialRepository.findByMaterialCodeIgnoreCaseAndIsDeletedFalse(bom.getMaterialCode());
            Material mat = null;
            String scrapCostCenter = request.getScrapCostCenter();
            if (matOpt.isPresent()) {
                mat = matOpt.get();
                mat.setCurrentStock(mat.getCurrentStock().subtract(actualQty));
                mat.setUpdatedAt(Instant.now());
                materialRepository.save(mat);
                if (scrapCostCenter == null || scrapCostCenter.isBlank()) {
                    scrapCostCenter = mat.getScrapCostCenter();
                }
            }
            if (scrapCostCenter == null || scrapCostCenter.isBlank()) {
                scrapCostCenter = "CC-SCRAP-MACHINING";
            }

            // Also decrement from material lot if lotNumber provided
            if (request.getLotNumber() != null && !request.getLotNumber().isBlank()) {
                final BigDecimal finalDeduction = actualQty;
                materialLotRepository.findByLotNumber(request.getLotNumber()).ifPresent(lot -> {
                    lot.setQuantity(lot.getQuantity().subtract(finalDeduction));
                    lot.setUpdatedAt(Instant.now());
                    materialLotRepository.save(lot);
                });
            }

            MaterialConsumptionRecord record = new MaterialConsumptionRecord();
            record.setProductionOrder(order);
            record.setPlant(order.getPlant());
            record.setMaterial(mat);
            record.setMaterialCode(bom.getMaterialCode());
            record.setMaterialName(bom.getMaterialName());
            record.setLotNumber(request.getLotNumber());
            record.setGoodPiecesProduced(goodIncrement);
            record.setScrapPiecesProduced(scrapIncrement);
            record.setScrapReasonCode(request.getScrapReasonCode());
            record.setScrapCostCenter(scrapCostCenter);
            record.setTheoreticalQuantity(theoreticalQty);
            record.setActualQuantity(actualQty);
            record.setVariancePercentage(variancePct);
            record.setVarianceAlertTriggered(varianceAlert);
            record.setUom(bom.getUom());
            record.setRecordedById(actor != null ? actor.getId() : null);
            record.setRecordedAt(Instant.now());

            MaterialConsumptionRecord saved = consumptionRecordRepository.save(record);
            recordDtos.add(MaterialConsumptionRecordDto.from(saved));

            if (varianceAlert) {
                log.warn("CONSUMPTION VARIANCE ALERT: Material '{}' on order '{}' deviated by {}% (Theoretical: {}, Actual: {})",
                        bom.getMaterialCode(), order.getOrderNumber(), variancePct, theoreticalQty, actualQty);

                auditRecordingService.record(
                        actor != null ? actor.getId() : null,
                        "MATERIAL_CONSUMPTION_VARIANCE_ALERT",
                        "MaterialConsumptionRecord",
                        saved.getId(),
                        Map.of("theoreticalQuantity", theoreticalQty, "expectedVarianceLimitPct", 5.0),
                        Map.of("actualQuantity", actualQty, "variancePercentage", variancePct, "materialCode", bom.getMaterialCode())
                );
            }
        }

        // Increment order good and scrap quantities
        order.setGoodQuantity(order.getGoodQuantity() + goodIncrement);
        order.setScrapQuantity(order.getScrapQuantity() + scrapIncrement);
        order.setUpdatedBy(actor != null ? actor.getId() : null);
        order.setUpdatedAt(Instant.now());
        productionOrderRepository.save(order);

        auditRecordingService.record(
                actor != null ? actor.getId() : null,
                "MATERIAL_BACKFLUSH_EXECUTED",
                "ProductionOrder",
                order.getId(),
                Map.of("orderNumber", order.getOrderNumber()),
                Map.of("goodIncrement", goodIncrement, "scrapIncrement", scrapIncrement, "recordsCreated", recordDtos.size())
        );

        return recordDtos;
    }

    @Transactional(readOnly = true)
    public List<MaterialConsumptionRecordDto> getRecentConsumptionRecords(UUID orderId) {
        if (orderId != null) {
            return consumptionRecordRepository.findByProductionOrderIdOrderByRecordedAtDesc(orderId).stream()
                    .map(MaterialConsumptionRecordDto::from)
                    .toList();
        }
        return consumptionRecordRepository.findTop50ByOrderByRecordedAtDesc().stream()
                .map(MaterialConsumptionRecordDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MaterialConsumptionRecordDto> getVarianceAlerts() {
        return consumptionRecordRepository.findByVarianceAlertTriggeredTrueOrderByRecordedAtDesc().stream()
                .map(MaterialConsumptionRecordDto::from)
                .toList();
    }
}
