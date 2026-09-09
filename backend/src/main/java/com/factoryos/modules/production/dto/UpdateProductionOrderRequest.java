package com.factoryos.modules.production.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UpdateProductionOrderRequest {

    @Size(max = 100, message = "Product code cannot exceed 100 characters")
    private String productCode;

    @Size(max = 500, message = "Product description cannot exceed 500 characters")
    private String productDescription;

    @Min(value = 1, message = "Planned quantity must be greater than 0")
    private Integer plannedQuantity;

    @NotNull(message = "Expected version is required for optimistic locking")
    private Long expectedVersion;

    public UpdateProductionOrderRequest() {
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

    public Long getExpectedVersion() {
        return expectedVersion;
    }

    public void setExpectedVersion(Long expectedVersion) {
        this.expectedVersion = expectedVersion;
    }
}
