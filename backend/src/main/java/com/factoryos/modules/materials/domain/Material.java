package com.factoryos.modules.materials.domain;

import com.factoryos.modules.tenant.domain.Plant;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "materials")
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plant_id")
    private Plant plant;

    @Column(name = "material_code", nullable = false, unique = true, length = 100)
    private String materialCode;

    @Column(name = "material_name", nullable = false)
    private String materialName;

    @Column(nullable = false, length = 64)
    private String category = "RAW_MATERIAL";

    @Column(nullable = false, length = 32)
    private String uom = "KG";

    @Column(name = "current_stock", nullable = false, precision = 14, scale = 4)
    private BigDecimal currentStock = BigDecimal.ZERO;

    @Column(name = "minimum_stock", nullable = false, precision = 14, scale = 4)
    private BigDecimal minimumStock = BigDecimal.valueOf(50.0);

    @Column(name = "standard_cost", nullable = false, precision = 14, scale = 4)
    private BigDecimal standardCost = BigDecimal.valueOf(10.0);

    @Column(name = "scrap_cost_center", nullable = false, length = 64)
    private String scrapCostCenter = "CC-SCRAP-MACHINING";

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Version
    @Column(nullable = false)
    private long version = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Material() {
    }

    public Material(String materialCode, String materialName, String category, String uom, BigDecimal currentStock, BigDecimal standardCost, String scrapCostCenter) {
        this.materialCode = materialCode;
        this.materialName = materialName;
        this.category = category;
        this.uom = uom;
        this.currentStock = currentStock;
        this.standardCost = standardCost;
        this.scrapCostCenter = scrapCostCenter;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Plant getPlant() {
        return plant;
    }

    public void setPlant(Plant plant) {
        this.plant = plant;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getUom() {
        return uom;
    }

    public void setUom(String uom) {
        this.uom = uom;
    }

    public BigDecimal getCurrentStock() {
        return currentStock;
    }

    public void setCurrentStock(BigDecimal currentStock) {
        this.currentStock = currentStock;
    }

    public BigDecimal getMinimumStock() {
        return minimumStock;
    }

    public void setMinimumStock(BigDecimal minimumStock) {
        this.minimumStock = minimumStock;
    }

    public BigDecimal getStandardCost() {
        return standardCost;
    }

    public void setStandardCost(BigDecimal standardCost) {
        this.standardCost = standardCost;
    }

    public String getScrapCostCenter() {
        return scrapCostCenter;
    }

    public void setScrapCostCenter(String scrapCostCenter) {
        this.scrapCostCenter = scrapCostCenter;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
