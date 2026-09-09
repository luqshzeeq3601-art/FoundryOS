package com.factoryos.modules.telemetry.dto;

import com.factoryos.modules.telemetry.domain.MachineTagMapping;
import com.factoryos.modules.telemetry.domain.ProtocolType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class TagMappingDto {

    private UUID id;
    private UUID machineId;
    private String machineName;
    private String tagName;
    private ProtocolType protocol;
    private String tagAddress;
    private String dataType;
    private String unitOfMeasure;
    private BigDecimal scaleFactor;
    private boolean isActive;
    private Instant createdAt;

    public TagMappingDto() {
    }

    public static TagMappingDto from(MachineTagMapping mapping) {
        TagMappingDto dto = new TagMappingDto();
        dto.setId(mapping.getId());
        dto.setMachineId(mapping.getMachine().getId());
        dto.setMachineName(mapping.getMachine().getName());
        dto.setTagName(mapping.getTagName());
        dto.setProtocol(mapping.getProtocol());
        dto.setTagAddress(mapping.getTagAddress());
        dto.setDataType(mapping.getDataType());
        dto.setUnitOfMeasure(mapping.getUnitOfMeasure());
        dto.setScaleFactor(mapping.getScaleFactor());
        dto.setActive(mapping.isActive());
        dto.setCreatedAt(mapping.getCreatedAt());
        return dto;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
