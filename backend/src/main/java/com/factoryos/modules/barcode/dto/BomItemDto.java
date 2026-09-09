package com.factoryos.modules.barcode.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class BomItemDto {

    private UUID id;
    private String productCode;
    private String materialCode;
    private String materialName;
    private BigDecimal requiredQuantityPerUnit;
    private String uom;

    public BomItemDto() {
    }

    public BomItemDto(UUID id, String productCode, String materialCode, String materialName, BigDecimal requiredQuantityPerUnit, String uom) {
        this.id = id;
        this.productCode = productCode;
        this.materialCode = materialCode;
        this.materialName = materialName;
        this.requiredQuantityPerUnit = requiredQuantityPerUnit;
        this.uom = uom;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
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

    public BigDecimal getRequiredQuantityPerUnit() {
        return requiredQuantityPerUnit;
    }

    public void setRequiredQuantityPerUnit(BigDecimal requiredQuantityPerUnit) {
        this.requiredQuantityPerUnit = requiredQuantityPerUnit;
    }

    public String getUom() {
        return uom;
    }

    public void setUom(String uom) {
        this.uom = uom;
    }
}
