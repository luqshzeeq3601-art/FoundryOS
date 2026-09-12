package com.factoryos.modules.erp.dto;

import com.factoryos.modules.erp.domain.ErpType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class CreateErpConnectorRequest {

    private UUID plantId;

    @NotBlank(message = "Connector name is required")
    private String name;

    @NotNull(message = "ERP type is required")
    private ErpType erpType = ErpType.SAP_S4HANA;

    @NotBlank(message = "Base URL is required")
    private String baseUrl;

    private String authType = "BASIC";
    private String apiKeyOrUser;
    private String secretOrToken;
    private String clientId;
    private String companyIdOrClient;
    private int syncIntervalSeconds = 300;
    private boolean isAutoSyncEnabled = true;

    public CreateErpConnectorRequest() {
    }

    public UUID getPlantId() {
        return plantId;
    }

    public void setPlantId(UUID plantId) {
        this.plantId = plantId;
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

    public String getSecretOrToken() {
        return secretOrToken;
    }

    public void setSecretOrToken(String secretOrToken) {
        this.secretOrToken = secretOrToken;
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

    public boolean isAutoSyncEnabled() {
        return isAutoSyncEnabled;
    }

    public void setAutoSyncEnabled(boolean autoSyncEnabled) {
        isAutoSyncEnabled = autoSyncEnabled;
    }
}
