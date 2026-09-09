package com.factoryos.modules.production.dto;

import com.factoryos.modules.production.domain.ProductionOrder;
import com.factoryos.modules.production.domain.ProductionOrderStatus;

import java.time.Instant;
import java.util.UUID;

public class ProductionOrderDto {
    private UUID id;
    private String orderNumber;
    private UUID machineId;
    private String machineName;
    private String productCode;
    private String productDescription;
    private int plannedQuantity;
    private int goodQuantity;
    private int scrapQuantity;
    private ProductionOrderStatus status;
    private Instant startedAt;
    private Instant completedAt;
    private Instant closedAt;
    private String closureNote;
    private Instant createdAt;
    private Instant updatedAt;
    private long version;

    public ProductionOrderDto() {
    }

    public static ProductionOrderDto from(ProductionOrder order) {
        ProductionOrderDto dto = new ProductionOrderDto();
        dto.setId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setMachineId(order.getMachine().getId());
        dto.setMachineName(order.getMachine().getName());
        dto.setProductCode(order.getProductCode());
        dto.setProductDescription(order.getProductDescription());
        dto.setPlannedQuantity(order.getPlannedQuantity());
        dto.setGoodQuantity(order.getGoodQuantity());
        dto.setScrapQuantity(order.getScrapQuantity());
        dto.setStatus(order.getStatus());
        dto.setStartedAt(order.getStartedAt());
        dto.setCompletedAt(order.getCompletedAt());
        dto.setClosedAt(order.getClosedAt());
        dto.setClosureNote(order.getClosureNote());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());
        dto.setVersion(order.getVersion());
        return dto;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public UUID getMachineId() {
        return machineId;
    }

    public void setMachineId(UUID machineId) {
        this.machineId = machineId;
    }

    public String getMachineName() {
        return machineName;
    }

    public void setMachineName(String machineName) {
        this.machineName = machineName;
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

    public int getPlannedQuantity() {
        return plannedQuantity;
    }

    public void setPlannedQuantity(int plannedQuantity) {
        this.plannedQuantity = plannedQuantity;
    }

    public int getGoodQuantity() {
        return goodQuantity;
    }

    public void setGoodQuantity(int goodQuantity) {
        this.goodQuantity = goodQuantity;
    }

    public int getScrapQuantity() {
        return scrapQuantity;
    }

    public void setScrapQuantity(int scrapQuantity) {
        this.scrapQuantity = scrapQuantity;
    }

    public ProductionOrderStatus getStatus() {
        return status;
    }

    public void setStatus(ProductionOrderStatus status) {
        this.status = status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    public String getClosureNote() {
        return closureNote;
    }

    public void setClosureNote(String closureNote) {
        this.closureNote = closureNote;
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

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }
}
