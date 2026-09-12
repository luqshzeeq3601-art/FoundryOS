package com.factoryos.modules.materials.dto;

import com.factoryos.modules.materials.domain.Material;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class MaterialDto {

    private UUID id;
    private UUID plantId;
    private String plantName;
    private String materialCode;
    private String materialName;
    private String category;
    private String uom;
    private BigDecimal currentStock;
    private BigDecimal minimumStock;
    private BigDecimal standardCost;
    private String scrapCostCenter;
    private boolean isLowStock;
    private Instant createdAt;
    private Instant updatedAt;

    public MaterialDto() {
    }

    public static MaterialDto from(Material m) {
        MaterialDto dto = new MaterialDto();
        dto.setId(m.getId());
        if (m.getPlant() != null) {
            dto.setPlantId(m.getPlant().getId());
            dto.setPlantName(m.getPlant().getName());
        }
        dto.setMaterialCode(m.getMaterialCode());
        dto.setMaterialName(m.getMaterialName());
        dto.setCategory(m.getCategory());
        dto.setUom(m.getUom());
        dto.setCurrentStock(m.getCurrentStock());
        dto.setMinimumStock(m.getMinimumStock());
        dto.setStandardCost(m.getStandardCost());
        dto.setScrapCostCenter(m.getScrapCostCenter());
        dto.setLowStock(m.getCurrentStock().compareTo(m.getMinimumStock()) <= 0);
        dto.setCreatedAt(m.getCreatedAt());
        dto.setUpdatedAt(m.getUpdatedAt());
        return dto;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPlantId() {
        return plantId;
    }

    public void setPlantId(UUID plantId) {
        this.plantId = plantId;
    }

    public String getPlantName() {
        return plantName;
    }

    public void setPlantName(String plantName) {
        this.plantName = plantName;
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

    public boolean isLowStock() {
        return isLowStock;
    }

    public void setLowStock(boolean lowStock) {
        isLowStock = lowStock;
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
