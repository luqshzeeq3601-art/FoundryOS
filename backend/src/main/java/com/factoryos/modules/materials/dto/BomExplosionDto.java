package com.factoryos.modules.materials.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class BomExplosionDto {

    private String productCode;
    private int plannedQuantity;
    private boolean allMaterialsInStock;
    private BigDecimal totalEstimatedMaterialCost = BigDecimal.ZERO;
    private List<BomComponentItem> components = new ArrayList<>();

    public BomExplosionDto() {
    }

    public BomExplosionDto(String productCode, int plannedQuantity) {
        this.productCode = productCode;
        this.plannedQuantity = plannedQuantity;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public int getPlannedQuantity() {
        return plannedQuantity;
    }

    public void setPlannedQuantity(int plannedQuantity) {
        this.plannedQuantity = plannedQuantity;
    }

    public boolean isAllMaterialsInStock() {
        return allMaterialsInStock;
    }

    public void setAllMaterialsInStock(boolean allMaterialsInStock) {
        this.allMaterialsInStock = allMaterialsInStock;
    }

    public BigDecimal getTotalEstimatedMaterialCost() {
        return totalEstimatedMaterialCost;
    }

    public void setTotalEstimatedMaterialCost(BigDecimal totalEstimatedMaterialCost) {
        this.totalEstimatedMaterialCost = totalEstimatedMaterialCost;
    }

    public List<BomComponentItem> getComponents() {
        return components;
    }

    public void setComponents(List<BomComponentItem> components) {
        this.components = components;
    }

    public static class BomComponentItem {
        private String materialCode;
        private String materialName;
        private BigDecimal requiredPerUnit;
        private BigDecimal totalRequiredQuantity;
        private BigDecimal currentStock;
        private boolean stockSufficient;
        private String uom;
        private String scrapCostCenter;
        private BigDecimal unitCost;
        private BigDecimal totalCost;

        public BomComponentItem() {
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

        public BigDecimal getRequiredPerUnit() {
            return requiredPerUnit;
        }

        public void setRequiredPerUnit(BigDecimal requiredPerUnit) {
            this.requiredPerUnit = requiredPerUnit;
        }

        public BigDecimal getTotalRequiredQuantity() {
            return totalRequiredQuantity;
        }

        public void setTotalRequiredQuantity(BigDecimal totalRequiredQuantity) {
            this.totalRequiredQuantity = totalRequiredQuantity;
        }

        public BigDecimal getCurrentStock() {
            return currentStock;
        }

        public void setCurrentStock(BigDecimal currentStock) {
            this.currentStock = currentStock;
        }

        public boolean isStockSufficient() {
            return stockSufficient;
        }

        public void setStockSufficient(boolean stockSufficient) {
            this.stockSufficient = stockSufficient;
        }

        public String getUom() {
            return uom;
        }

        public void setUom(String uom) {
            this.uom = uom;
        }

        public String getScrapCostCenter() {
            return scrapCostCenter;
        }

        public void setScrapCostCenter(String scrapCostCenter) {
            this.scrapCostCenter = scrapCostCenter;
        }

        public BigDecimal getUnitCost() {
            return unitCost;
        }

        public void setUnitCost(BigDecimal unitCost) {
            this.unitCost = unitCost;
        }

        public BigDecimal getTotalCost() {
            return totalCost;
        }

        public void setTotalCost(BigDecimal totalCost) {
            this.totalCost = totalCost;
        }
    }
}
