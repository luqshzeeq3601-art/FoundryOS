package com.factoryos.modules.production.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class UpdateProductionProgressRequest {

    @NotNull(message = "Good quantity is required")
    @Min(value = 0, message = "Good quantity cannot be negative")
    private Integer goodQuantity;

    @NotNull(message = "Scrap quantity is required")
    @Min(value = 0, message = "Scrap quantity cannot be negative")
    private Integer scrapQuantity;

    @NotNull(message = "Expected version is required for optimistic locking")
    private Long expectedVersion;

    public UpdateProductionProgressRequest() {
    }

    public Integer getGoodQuantity() {
        return goodQuantity;
    }

    public void setGoodQuantity(Integer goodQuantity) {
        this.goodQuantity = goodQuantity;
    }

    public Integer getScrapQuantity() {
        return scrapQuantity;
    }

    public void setScrapQuantity(Integer scrapQuantity) {
        this.scrapQuantity = scrapQuantity;
    }

    public Long getExpectedVersion() {
        return expectedVersion;
    }

    public void setExpectedVersion(Long expectedVersion) {
        this.expectedVersion = expectedVersion;
    }
}
