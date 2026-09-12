package com.factoryos.modules.erp.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class InboundErpOrderDto {

    private String erpOrderId;
    private String orderNumber;
    private String productCode;
    private String productDescription;
    private int plannedQuantity = 100;
    private String targetMachineCode;
    private String plantCode;
    private String erpSystem;
    private String erpBatchNumber;
    private String unitOfMeasure;
    private String priority;
    private Instant scheduledStartDate;
    private Instant scheduledEndDate;
    private List<ErpBomItemDto> bomItems;

    public InboundErpOrderDto() {
    }

    public String getErpOrderId() {
        return erpOrderId;
    }

    public void setErpOrderId(String erpOrderId) {
        this.erpOrderId = erpOrderId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getProductDescription() {
        return productDescription;
    }

    public void setProductDescription(String productDescription) {
        this.productDescription = productDescription;
    }

    public String getProductName() {
        return productDescription;
    }

    public void setProductName(String productName) {
        this.productDescription = productName;
    }

    public int getPlannedQuantity() {
        return plannedQuantity;
    }

    public void setPlannedQuantity(int plannedQuantity) {
        this.plannedQuantity = plannedQuantity;
    }

    public BigDecimal getTargetQuantity() {
        return BigDecimal.valueOf(plannedQuantity);
    }

    public void setTargetQuantity(BigDecimal targetQuantity) {
        this.plannedQuantity = targetQuantity != null ? targetQuantity.intValue() : 100;
    }

    public String getTargetMachineCode() {
        return targetMachineCode;
    }

    public void setTargetMachineCode(String targetMachineCode) {
        this.targetMachineCode = targetMachineCode;
    }

    public String getPlantCode() {
        return plantCode;
    }

    public void setPlantCode(String plantCode) {
        this.plantCode = plantCode;
    }

    public String getErpSystem() {
        return erpSystem;
    }

    public void setErpSystem(String erpSystem) {
        this.erpSystem = erpSystem;
    }

    public String getErpBatchNumber() {
        return erpBatchNumber;
    }

    public void setErpBatchNumber(String erpBatchNumber) {
        this.erpBatchNumber = erpBatchNumber;
    }

    public String getBatchNumber() {
        return erpBatchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.erpBatchNumber = batchNumber;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public Instant getScheduledStartDate() {
        return scheduledStartDate;
    }

    public void setScheduledStartDate(Instant scheduledStartDate) {
        this.scheduledStartDate = scheduledStartDate;
    }

    public Instant getScheduledEndDate() {
        return scheduledEndDate;
    }

    public void setScheduledEndDate(Instant scheduledEndDate) {
        this.scheduledEndDate = scheduledEndDate;
    }

    public List<ErpBomItemDto> getBomItems() {
        return bomItems;
    }

    public void setBomItems(List<ErpBomItemDto> bomItems) {
        this.bomItems = bomItems;
    }

    public static class ErpBomItemDto {
        private String materialCode;
        private String materialDescription;
        private double quantityPerUnit;
        private String unitOfMeasure;

        public ErpBomItemDto() {}

        public String getMaterialCode() {
            return materialCode;
        }

        public void setMaterialCode(String materialCode) {
            this.materialCode = materialCode;
        }

        public String getMaterialDescription() {
            return materialDescription;
        }

        public void setMaterialDescription(String materialDescription) {
            this.materialDescription = materialDescription;
        }

        public double getQuantityPerUnit() {
            return quantityPerUnit;
        }

        public void setQuantityPerUnit(double quantityPerUnit) {
            this.quantityPerUnit = quantityPerUnit;
        }

        public String getUnitOfMeasure() {
            return unitOfMeasure;
        }

        public void setUnitOfMeasure(String unitOfMeasure) {
            this.unitOfMeasure = unitOfMeasure;
        }
    }
}
