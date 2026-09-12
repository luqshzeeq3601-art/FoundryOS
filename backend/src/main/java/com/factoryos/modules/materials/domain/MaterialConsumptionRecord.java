package com.factoryos.modules.materials.domain;

import com.factoryos.modules.production.domain.ProductionOrder;
import com.factoryos.modules.tenant.domain.Plant;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "material_consumption_records")
public class MaterialConsumptionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "production_order_id", nullable = false)
    private ProductionOrder productionOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plant_id")
    private Plant plant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id")
    private Material material;

    @Column(name = "material_code", nullable = false, length = 100)
    private String materialCode;

    @Column(name = "material_name", nullable = false)
    private String materialName;

    @Column(name = "lot_number", length = 80)
    private String lotNumber;

    @Column(name = "good_pieces_produced", nullable = false)
    private int goodPiecesProduced = 0;

    @Column(name = "scrap_pieces_produced", nullable = false)
    private int scrapPiecesProduced = 0;

    @Column(name = "scrap_reason_code", length = 64)
    private String scrapReasonCode;

    @Column(name = "scrap_cost_center", nullable = false, length = 64)
    private String scrapCostCenter = "CC-SCRAP-MACHINING";

    @Column(name = "theoretical_quantity", nullable = false, precision = 14, scale = 4)
    private BigDecimal theoreticalQuantity;

    @Column(name = "actual_quantity", nullable = false, precision = 14, scale = 4)
    private BigDecimal actualQuantity;

    @Column(name = "variance_percentage", nullable = false, precision = 8, scale = 4)
    private BigDecimal variancePercentage = BigDecimal.ZERO;

    @Column(name = "variance_alert_triggered", nullable = false)
    private boolean varianceAlertTriggered = false;

    @Column(nullable = false, length = 32)
    private String uom = "KG";

    @Column(name = "recorded_by_id")
    private UUID recordedById;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt = Instant.now();

    public MaterialConsumptionRecord() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ProductionOrder getProductionOrder() {
        return productionOrder;
    }

    public void setProductionOrder(ProductionOrder productionOrder) {
        this.productionOrder = productionOrder;
    }

    public Plant getPlant() {
        return plant;
    }

    public void setPlant(Plant plant) {
        this.plant = plant;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
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

    public UUID getRecordedById() {
        return recordedById;
    }

    public void setRecordedById(UUID recordedById) {
        this.recordedById = recordedById;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }
}
