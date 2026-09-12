package com.factoryos.modules.erp.domain;

import com.factoryos.modules.tenant.domain.Plant;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "erp_connectors")
public class ErpConnector {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plant_id")
    private Plant plant;

    @Column(nullable = false, length = 128)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "erp_type", nullable = false, length = 32)
    private ErpType erpType = ErpType.SAP_S4HANA;

    @Column(name = "base_url", nullable = false, length = 512)
    private String baseUrl;

    @Column(name = "auth_type", nullable = false, length = 32)
    private String authType = "BASIC";

    @Column(name = "api_key_or_user", length = 256)
    private String apiKeyOrUser;

    @Column(name = "secret_or_token", length = 1024)
    private String secretOrToken;

    @Column(name = "client_id", length = 128)
    private String clientId;

    @Column(name = "company_id_or_client", length = 64)
    private String companyIdOrClient;

    @Column(name = "sync_interval_seconds", nullable = false)
    private int syncIntervalSeconds = 300;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "is_auto_sync_enabled", nullable = false)
    private boolean isAutoSyncEnabled = true;

    @Column(name = "health_status", nullable = false, length = 32)
    private String healthStatus = "HEALTHY";

    @Column(name = "last_health_check_at")
    private Instant lastHealthCheckAt;

    @Column(name = "last_sync_at")
    private Instant lastSyncAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Version
    @Column(nullable = false)
    private long version = 0L;

    public ErpConnector() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Plant getPlant() {
        return plant;
    }

    public void setPlant(Plant plant) {
        this.plant = plant;
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

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }
}
