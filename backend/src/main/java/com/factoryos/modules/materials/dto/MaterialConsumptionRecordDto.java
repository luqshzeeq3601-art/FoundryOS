package com.factoryos.modules.materials.dto;

import com.factoryos.modules.materials.domain.MaterialConsumptionRecord;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class MaterialConsumptionRecordDto {

    private UUID id;
    private UUID productionOrderId;
    private String orderNumber;
    private UUID plantId;
    private String plantCode;
    private UUID materialId;
    private String materialCode;
    private String materialName;
    private String lotNumber;
    private int goodPiecesProduced;
    private int scrapPiecesProduced;
    private String scrapReasonCode;
    private String scrapCostCenter;
    private BigDecimal theoreticalQuantity;
    private BigDecimal actualQuantity;
    private BigDecimal variancePercentage;
    private boolean varianceAlertTriggered;
    private String uom;
    private Instant recordedAt;

    public MaterialConsumptionRecordDto() {
    }

    public static MaterialConsumptionRecordDto from(MaterialConsumptionRecord r) {
        MaterialConsumptionRecordDto dto = new MaterialConsumptionRecordDto();
        dto.setId(r.getId());
        if (r.getProductionOrder() != null) {
            dto.setProductionOrderId(r.getProductionOrder().getId());
            dto.setOrderNumber(r.getProductionOrder().getOrderNumber());
        }
        if (r.getPlant() != null) {
            dto.setPlantId(r.getPlant().getId());
            dto.setPlantCode(r.getPlant().getCode());
        }
        if (r.getMaterial() != null) {
            dto.setMaterialId(r.getMaterial().getId());
        }
        dto.setMaterialCode(r.getMaterialCode());
        dto.setMaterialName(r.getMaterialName());
        dto.setLotNumber(r.getLotNumber());
        dto.setGoodPiecesProduced(r.getGoodPiecesProduced());
        dto.setScrapPiecesProduced(r.getScrapPiecesProduced());
        dto.setScrapReasonCode(r.getScrapReasonCode());
        dto.setScrapCostCenter(r.getScrapCostCenter());
        dto.setTheoreticalQuantity(r.getTheoreticalQuantity());
        dto.setActualQuantity(r.getActualQuantity());
        dto.setVariancePercentage(r.getVariancePercentage());
        dto.setVarianceAlertTriggered(r.isVarianceAlertTriggered());
        dto.setUom(r.getUom());
        dto.setRecordedAt(r.getRecordedAt());
        return dto;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getProductionOrderId() {
        return productionOrderId;
    }

    public void setProductionOrderId(UUID productionOrderId) {
        this.productionOrderId = productionOrderId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public UUID getPlantId() {
        return plantId;
    }

    public void setPlantId(UUID plantId) {
        this.plantId = plantId;
    }

    public String getPlantCode() {
        return plantCode;
    }

    public void setPlantCode(String plantCode) {
        this.plantCode = plantCode;
    }

    public UUID getMaterialId() {
        return materialId;
    }

    public void setMaterialId(UUID materialId) {
        this.materialId = materialId;
    }

    public String getMaterialCode() {
        return materialCode;
    }

    public void setMaterialCode(String materialCode) {
        this.materialCode = materialCode;
    }

    public String getMaterialName() {
        return materialName;
    }

    public void setMaterialName(String materialName) {
        this.materialName = materialName;
    }

    public String getLotNumber() {
        return lotNumber;
    }

    public void setLotNumber(String lotNumber) {
        this.lotNumber = lotNumber;
    }

    public int getGoodPiecesProduced() {
        return goodPiecesProduced;
    }

    public void setGoodPiecesProduced(int goodPiecesProduced) {
        this.goodPiecesProduced = goodPiecesProduced;
    }

    public int getScrapPiecesProduced() {
        return scrapPiecesProduced;
    }

    public void setScrapPiecesProduced(int scrapPiecesProduced) {
        this.scrapPiecesProduced = scrapPiecesProduced;
    }

    public String getScrapReasonCode() {
        return scrapReasonCode;
    }

    public void setScrapReasonCode(String scrapReasonCode) {
        this.scrapReasonCode = scrapReasonCode;
    }

    public String getScrapCostCenter() {
        return scrapCostCenter;
    }

    public void setScrapCostCenter(String scrapCostCenter) {
        this.scrapCostCenter = scrapCostCenter;
    }

    public BigDecimal getTheoreticalQuantity() {
        return theoreticalQuantity;
    }

    public void setTheoreticalQuantity(BigDecimal theoreticalQuantity) {
        this.theoreticalQuantity = theoreticalQuantity;
    }

    public BigDecimal getActualQuantity() {
        return actualQuantity;
    }

    public void setActualQuantity(BigDecimal actualQuantity) {
        this.actualQuantity = actualQuantity;
    }

    public BigDecimal getVariancePercentage() {
        return variancePercentage;
    }

    public void setVariancePercentage(BigDecimal variancePercentage) {
        this.variancePercentage = variancePercentage;
    }

    public boolean isVarianceAlertTriggered() {
        return varianceAlertTriggered;
    }

    public void setVarianceAlertTriggered(boolean varianceAlertTriggered) {
        this.varianceAlertTriggered = varianceAlertTriggered;
    }

    public String getUom() {
        return uom;
    }

    public void setUom(String uom) {
        this.uom = uom;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }
}
