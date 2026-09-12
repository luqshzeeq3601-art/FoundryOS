package com.factoryos.modules.erp.dto;

import com.factoryos.modules.erp.domain.ErpSyncDirection;
import com.factoryos.modules.erp.domain.ErpSyncLog;

import java.time.Instant;
import java.util.UUID;

public class ErpSyncLogDto {

    private UUID id;
    private UUID connectorId;
    private String connectorName;
    private UUID plantId;
    private String plantCode;
    private ErpSyncDirection syncDirection;
    private String entityType;
    private UUID entityId;
    private String erpReferenceId;
    private String status;
    private String payloadJson;
    private String responseJson;
    private String errorMessage;
    private int retryCount;
    private long durationMs;
    private Instant syncedAt;

    public ErpSyncLogDto() {
    }

    public static ErpSyncLogDto from(ErpSyncLog log) {
        ErpSyncLogDto dto = new ErpSyncLogDto();
        dto.setId(log.getId());
        if (log.getConnector() != null) {
            dto.setConnectorId(log.getConnector().getId());
            dto.setConnectorName(log.getConnector().getName());
        }
        if (log.getPlant() != null) {
            dto.setPlantId(log.getPlant().getId());
            dto.setPlantCode(log.getPlant().getCode());
        }
        dto.setSyncDirection(log.getSyncDirection());
        dto.setEntityType(log.getEntityType());
        dto.setEntityId(log.getEntityId());
        dto.setErpReferenceId(log.getErpReferenceId());
        dto.setStatus(log.getStatus());
        dto.setPayloadJson(log.getPayloadJson());
        dto.setResponseJson(log.getResponseJson());
        dto.setErrorMessage(log.getErrorMessage());
        dto.setRetryCount(log.getRetryCount());
        dto.setDurationMs(log.getDurationMs());
        dto.setSyncedAt(log.getSyncedAt());
        return dto;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getConnectorId() {
        return connectorId;
    }

    public void setConnectorId(UUID connectorId) {
        this.connectorId = connectorId;
    }

    public String getConnectorName() {
        return connectorName;
    }

    public void setConnectorName(String connectorName) {
        this.connectorName = connectorName;
    }

    public UUID getPlantId() {
        return plantId;
    }

    public void setPlantId(UUID plantId) {
        this.plantId = plantId;
    }

    public String getPlantCode() {
        return plantCode;
    }

    public void setPlantCode(String plantCode) {
        this.plantCode = plantCode;
    }

    public ErpSyncDirection getSyncDirection() {
        return syncDirection;
    }

    public void setSyncDirection(ErpSyncDirection syncDirection) {
        this.syncDirection = syncDirection;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public void setEntityId(UUID entityId) {
        this.entityId = entityId;
    }

    public String getErpReferenceId() {
        return erpReferenceId;
    }

    public void setErpReferenceId(String erpReferenceId) {
        this.erpReferenceId = erpReferenceId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }

    public String getResponseJson() {
        return responseJson;
    }

    public void setResponseJson(String responseJson) {
        this.responseJson = responseJson;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public Instant getSyncedAt() {
        return syncedAt;
    }

    public void setSyncedAt(Instant syncedAt) {
        this.syncedAt = syncedAt;
    }
}