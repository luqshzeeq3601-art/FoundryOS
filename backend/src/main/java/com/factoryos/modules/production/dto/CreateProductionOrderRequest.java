package com.factoryos.modules.production.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class CreateProductionOrderRequest {

    @NotBlank(message = "Order number is required")
    @Size(max = 40, message = "Order number cannot exceed 40 characters")
    private String orderNumber;

    @NotNull(message = "Machine ID is required")
    private UUID machineId;

    @NotBlank(message = "Product code is required")
    @Size(max = 100, message = "Product code cannot exceed 100 characters")
    private String productCode;

    @Size(max = 500, message = "Product description cannot exceed 500 characters")
    private String productDescription;

    @NotNull(message = "Planned quantity is required")
    @Min(value = 1, message = "Planned quantity must be greater than 0")
    private Integer plannedQuantity;

    public CreateProductionOrderRequest() {
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

    public Integer getPlannedQuantity() {
        return plannedQuantity;
    }

    public void setPlannedQuantity(Integer plannedQuantity) {
        this.plannedQuantity = plannedQuantity;
    }
}
