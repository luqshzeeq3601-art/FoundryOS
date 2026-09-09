package com.factoryos.modules.telemetry.dto;

import com.factoryos.modules.telemetry.domain.ProtocolType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class CreateTagMappingRequest {

    @NotBlank(message = "Tag name is required")
    private String tagName;

    @NotNull(message = "Protocol is required")
    private ProtocolType protocol = ProtocolType.OPC_UA;

    @NotBlank(message = "Tag address/node ID is required")
    private String tagAddress;

    private String dataType = "DOUBLE";
    private String unitOfMeasure;
    private BigDecimal scaleFactor = BigDecimal.ONE;

    public CreateTagMappingRequest() {
    }

    public CreateTagMappingRequest(String tagName, ProtocolType protocol, String tagAddress, String dataType, String unitOfMeasure, BigDecimal scaleFactor) {
        this.tagName = tagName;
        this.protocol = protocol;
        this.tagAddress = tagAddress;
        this.dataType = dataType;
        this.unitOfMeasure = unitOfMeasure;
        this.scaleFactor = scaleFactor;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public ProtocolType getProtocol() {
        return protocol;
    }

    public void setProtocol(ProtocolType protocol) {
        this.protocol = protocol;
    }

    public String getTagAddress() {
        return tagAddress;
    }

    public void setTagAddress(String tagAddress) {
        this.tagAddress = tagAddress;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public BigDecimal getScaleFactor() {
        return scaleFactor;
    }

    public void setScaleFactor(BigDecimal scaleFactor) {
        this.scaleFactor = scaleFactor;
    }
}
