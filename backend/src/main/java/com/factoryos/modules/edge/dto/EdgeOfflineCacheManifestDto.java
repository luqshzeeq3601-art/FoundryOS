package com.factoryos.modules.edge.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EdgeOfflineCacheManifestDto {

    private String gatewayCode;
    private UUID plantId;
    private String plantName;
    private Instant manifestGeneratedAt;
    private List<CachedOrderDto> activeOrders = new ArrayList<>();
    private List<CachedMachineDto> machines = new ArrayList<>();
    private List<CachedBomItemDto> bomItems = new ArrayList<>();
    private List<CachedMaterialLotDto> materialLots = new ArrayList<>();

    public EdgeOfflineCacheManifestDto() {
    }

    public String getGatewayCode() {
        return gatewayCode;
    }

    public void setGatewayCode(String gatewayCode) {
        this.gatewayCode = gatewayCode;
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

    public Instant getManifestGeneratedAt() {
        return manifestGeneratedAt;
    }

    public void setManifestGeneratedAt(Instant manifestGeneratedAt) {
        this.manifestGeneratedAt = manifestGeneratedAt;
    }

    public List<CachedOrderDto> getActiveOrders() {
        return activeOrders;
    }

    public void setActiveOrders(List<CachedOrderDto> activeOrders) {
        this.activeOrders = activeOrders != null ? activeOrders : new ArrayList<>();
    }

    public List<CachedMachineDto> getMachines() {
        return machines;
    }

    public void setMachines(List<CachedMachineDto> machines) {
        this.machines = machines != null ? machines : new ArrayList<>();
    }

    public List<CachedBomItemDto> getBomItems() {
        return bomItems;
    }

    public void setBomItems(List<CachedBomItemDto> bomItems) {
        this.bomItems = bomItems != null ? bomItems : new ArrayList<>();
    }

    public List<CachedMaterialLotDto> getMaterialLots() {
        return materialLots;
    }

    public void setMaterialLots(List<CachedMaterialLotDto> materialLots) {
        this.materialLots = materialLots != null ? materialLots : new ArrayList<>();
    }

    public static class CachedOrderDto {
        private UUID id;
        private String orderNumber;
        private String productCode;
        private String productName;
        private int targetQuantity;
        private int goodQuantity;
        private int scrapQuantity;
        private String status;
        private UUID machineId;
        private String machineCode;

        public CachedOrderDto() {
        }

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public String getOrderNumber() { return orderNumber; }
        public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
        public String getProductCode() { return productCode; }
        public void setProductCode(String productCode) { this.productCode = productCode; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public int getTargetQuantity() { return targetQuantity; }
        public void setTargetQuantity(int targetQuantity) { this.targetQuantity = targetQuantity; }
        public int getGoodQuantity() { return goodQuantity; }
        public void setGoodQuantity(int goodQuantity) { this.goodQuantity = goodQuantity; }
        public int getScrapQuantity() { return scrapQuantity; }
        public void setScrapQuantity(int scrapQuantity) { this.scrapQuantity = scrapQuantity; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public UUID getMachineId() { return machineId; }
        public void setMachineId(UUID machineId) { this.machineId = machineId; }
        public String getMachineCode() { return machineCode; }
        public void setMachineCode(String machineCode) { this.machineCode = machineCode; }
    }

    public static class CachedMachineDto {
        private UUID id;
        private String machineCode;
        private String name;
        private String status;
        private UUID lineId;
        private String lineName;

        public CachedMachineDto() {
        }

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public String getMachineCode() { return machineCode; }
        public void setMachineCode(String machineCode) { this.machineCode = machineCode; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public UUID getLineId() { return lineId; }
        public void setLineId(UUID lineId) { this.lineId = lineId; }
        public String getLineName() { return lineName; }
        public void setLineName(String lineName) { this.lineName = lineName; }
    }

    public static class CachedBomItemDto {
        private UUID id;
        private String productCode;
        private String componentMaterialCode;
        private String componentDescription;
        private double quantityRequired;
        private String uom;

        public CachedBomItemDto() {
        }

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public String getProductCode() { return productCode; }
        public void setProductCode(String productCode) { this.productCode = productCode; }
        public String getComponentMaterialCode() { return componentMaterialCode; }
        public void setComponentMaterialCode(String componentMaterialCode) { this.componentMaterialCode = componentMaterialCode; }
        public String getComponentDescription() { return componentDescription; }
        public void setComponentDescription(String componentDescription) { this.componentDescription = componentDescription; }
        public double getQuantityRequired() { return quantityRequired; }
        public void setQuantityRequired(double quantityRequired) { this.quantityRequired = quantityRequired; }
        public String getUom() { return uom; }
        public void setUom(String uom) { this.uom = uom; }
    }

    public static class CachedMaterialLotDto {
        private UUID id;
        private String lotNumber;
        private String materialCode;
        private String materialName;
        private double quantityRemaining;
        private String uom;
        private String status;

        public CachedMaterialLotDto() {
        }

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public String getLotNumber() { return lotNumber; }
        public void setLotNumber(String lotNumber) { this.lotNumber = lotNumber; }
        public String getMaterialCode() { return materialCode; }
        public void setMaterialCode(String materialCode) { this.materialCode = materialCode; }
        public String getMaterialName() { return materialName; }
        public void setMaterialName(String materialName) { this.materialName = materialName; }
        public double getQuantityRemaining() { return quantityRemaining; }
        public void setQuantityRemaining(double quantityRemaining) { this.quantityRemaining = quantityRemaining; }
        public String getUom() { return uom; }
        public void setUom(String uom) { this.uom = uom; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
