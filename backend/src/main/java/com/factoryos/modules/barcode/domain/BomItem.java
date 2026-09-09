package com.factoryos.modules.barcode.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bill_of_materials", uniqueConstraints = {
    @UniqueConstraint(name = "uq_bom_product_material", columnNames = {"product_code", "material_code"})
})
public class BomItem {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "product_code", nullable = false, length = 100)
    private String productCode;

    @Column(name = "material_code", nullable = false, length = 100)
    private String materialCode;

    @Column(name = "material_name", nullable = false)
    private String materialName;

    @Column(name = "required_quantity_per_unit", nullable = false, precision = 14, scale = 4)
    private BigDecimal requiredQuantityPerUnit = BigDecimal.ONE;

    @Column(name = "uom", nullable = false, length = 32)
    private String uom = "PCS";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public BomItem() {
    }

    public BomItem(String productCode, String materialCode, String materialName, BigDecimal requiredQuantityPerUnit, String uom) {
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

    public Instant getCreatedAt() {
        return createdAt;
    }
}
