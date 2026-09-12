package com.factoryos.modules.erp.dto;

import com.factoryos.modules.erp.domain.ErpConnector;
import com.factoryos.modules.erp.domain.ErpType;

import java.time.Instant;
import java.util.UUID;

public class ErpConnectorDto {

    private UUID id;
    private UUID plantId;
    private String plantCode;
    private String plantName;
    private String name;
    private ErpType erpType;
    private String baseUrl;
    private String authType;
    private String apiKeyOrUser;
    private String clientId;
    private String companyIdOrClient;
    private int syncIntervalSeconds;
    private boolean isActive;
    private boolean isAutoSyncEnabled;
    private String healthStatus;
    private Instant lastHealthCheckAt;
    private Instant lastSyncAt;
    private Instant createdAt;
    private Instant updatedAt;

    public ErpConnectorDto() {
    }

    public static ErpConnectorDto from(ErpConnector c) {
        ErpConnectorDto dto = new ErpConnectorDto();
        dto.setId(c.getId());
        if (c.getPlant() != null) {
            dto.setPlantId(c.getPlant().getId());
            dto.setPlantCode(c.getPlant().getCode());
            dto.setPlantName(c.getPlant().getName());
        }
        dto.setName(c.getName());
        dto.setErpType(c.getErpType());
        dto.setBaseUrl(c.getBaseUrl());
        dto.setAuthType(c.getAuthType());
        dto.setApiKeyOrUser(c.getApiKeyOrUser());
        dto.setClientId(c.getClientId());
        dto.setCompanyIdOrClient(c.getCompanyIdOrClient());
        dto.setSyncIntervalSeconds(c.getSyncIntervalSeconds());
        dto.setActive(c.isActive());
        dto.setAutoSyncEnabled(c.isAutoSyncEnabled());
        dto.setHealthStatus(c.getHealthStatus());
        dto.setLastHealthCheckAt(c.getLastHealthCheckAt());
        dto.setLastSyncAt(c.getLastSyncAt());
        dto.setCreatedAt(c.getCreatedAt());
        dto.setUpdatedAt(c.getUpdatedAt());
        return dto;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public String getPlantName() {
        return plantName;
    }

    public void setPlantName(String plantName) {
        this.plantName = plantName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ErpType getErpType() {
        return erpType;
    }

    public void setErpType(ErpType erpType) {
        this.erpType = erpType;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getAuthType() {
        return authType;
    }

    public void setAuthType(String authType) {
        this.authType = authType;
    }

    public String getApiKeyOrUser() {
        return apiKeyOrUser;
    }

    public void setApiKeyOrUser(String apiKeyOrUser) {
        this.apiKeyOrUser = apiKeyOrUser;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getCompanyIdOrClient() {
        return companyIdOrClient;
    }

    public void setCompanyIdOrClient(String companyIdOrClient) {
        this.companyIdOrClient = companyIdOrClient;
    }

    public int getSyncIntervalSeconds() {
        return syncIntervalSeconds;
    }

    public void setSyncIntervalSeconds(int syncIntervalSeconds) {
        this.syncIntervalSeconds = syncIntervalSeconds;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public boolean isAutoSyncEnabled() {
        return isAutoSyncEnabled;
    }

    public void setAutoSyncEnabled(boolean autoSyncEnabled) {
        isAutoSyncEnabled = autoSyncEnabled;
    }

    public String getHealthStatus() {
        return healthStatus;
    }

    public void setHealthStatus(String healthStatus) {
        this.healthStatus = healthStatus;
    }

    public Instant getLastHealthCheckAt() {
        return lastHealthCheckAt;
    }

    public void setLastHealthCheckAt(Instant lastHealthCheckAt) {
        this.lastHealthCheckAt = lastHealthCheckAt;
    }

    public Instant getLastSyncAt() {
        return lastSyncAt;
    }

    public void setLastSyncAt(Instant lastSyncAt) {
        this.lastSyncAt = lastSyncAt;
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
}