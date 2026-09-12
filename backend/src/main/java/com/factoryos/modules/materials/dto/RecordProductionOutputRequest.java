package com.factoryos.modules.materials.dto;

import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.util.Map;

public class RecordProductionOutputRequest {

    @Min(0)
    private int incrementalGoodQuantity = 0;

    @Min(0)
    private int incrementalScrapQuantity = 0;

    private String scrapReasonCode;
    private String scrapCostCenter;
    private String lotNumber;
    private Map<String, BigDecimal> actualQuantities;

    public RecordProductionOutputRequest() {
    }

    public int getIncrementalGoodQuantity() {
        return incrementalGoodQuantity;
    }

    public void setIncrementalGoodQuantity(int incrementalGoodQuantity) {
        this.incrementalGoodQuantity = incrementalGoodQuantity;
    }

    public int getIncrementalScrapQuantity() {
        return incrementalScrapQuantity;
    }

    public void setIncrementalScrapQuantity(int incrementalScrapQuantity) {
        this.incrementalScrapQuantity = incrementalScrapQuantity;
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

    public String getLotNumber() {
        return lotNumber;
    }

    public void setLotNumber(String lotNumber) {
        this.lotNumber = lotNumber;
    }

    public Map<String, BigDecimal> getActualQuantities() {
        return actualQuantities;
    }

    public void setActualQuantities(Map<String, BigDecimal> actualQuantities) {
        this.actualQuantities = actualQuantities;
    }
}
