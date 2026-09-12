package com.factoryos.modules.erp.domain;

import com.factoryos.modules.production.domain.ProductionOrder;
import com.factoryos.modules.tenant.domain.Plant;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "erp_order_confirmations")
public class ErpOrderConfirmation {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "production_order_id", nullable = false)
    private ProductionOrder productionOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "connector_id")
    private ErpConnector connector;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plant_id")
    private Plant plant;

    @Column(name = "confirmation_number", nullable = false, unique = true, length = 64)
    private String confirmationNumber;

    @Column(name = "erp_order_id", nullable = false, length = 64)
    private String erpOrderId;

    @Column(name = "confirmed_good_qty", nullable = false)
    private int confirmedGoodQty = 0;

    @Column(name = "confirmed_scrap_qty", nullable = false)
    private int confirmedScrapQty = 0;

    @Column(name = "scrap_reason", length = 64)
    private String scrapReason;

    @Column(name = "labor_hours", nullable = false)
    private double laborHours = 0.0;

    @Column(name = "machine_hours", nullable = false)
    private double machineHours = 0.0;

    @Column(name = "erp_posting_status", nullable = false, length = 32)
    private String erpPostingStatus = "PENDING";

    @Column(name = "erp_document_number", length = 64)
    private String erpDocumentNumber;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "posted_at")
    private Instant postedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Version
    @Column(nullable = false)
    private long version = 0L;

    public ErpOrderConfirmation() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ProductionOrder getProductionOrder() {
        return productionOrder;
    }

    public void setProductionOrder(ProductionOrder productionOrder) {
        this.productionOrder = productionOrder;
    }

    public ErpConnector getConnector() {
        return connector;
    }

    public void setConnector(ErpConnector connector) {
        this.connector = connector;
    }

    public Plant getPlant() {
        return plant;
    }

    public void setPlant(Plant plant) {
        this.plant = plant;
    }

    public String getConfirmationNumber() {
        return confirmationNumber;
    }

    public void setConfirmationNumber(String confirmationNumber) {
        this.confirmationNumber = confirmationNumber;
    }

    public String getErpOrderId() {
        return erpOrderId;
    }

    public void setErpOrderId(String erpOrderId) {
        this.erpOrderId = erpOrderId;
    }

    public int getConfirmedGoodQty() {
        return confirmedGoodQty;
    }

    public void setConfirmedGoodQty(int confirmedGoodQty) {
        this.confirmedGoodQty = confirmedGoodQty;
    }

    public int getConfirmedScrapQty() {
        return confirmedScrapQty;
    }

    public void setConfirmedScrapQty(int confirmedScrapQty) {
        this.confirmedScrapQty = confirmedScrapQty;
    }

    public String getScrapReason() {
        return scrapReason;
    }

    public void setScrapReason(String scrapReason) {
        this.scrapReason = scrapReason;
    }

    public double getLaborHours() {
        return laborHours;
    }

    public void setLaborHours(double laborHours) {
        this.laborHours = laborHours;
    }

    public double getMachineHours() {
        return machineHours;
    }

    public void setMachineHours(double machineHours) {
        this.machineHours = machineHours;
    }

    public String getErpPostingStatus() {
        return erpPostingStatus;
    }

    public void setErpPostingStatus(String erpPostingStatus) {
        this.erpPostingStatus = erpPostingStatus;
    }

    public String getErpDocumentNumber() {
        return erpDocumentNumber;
    }

    public void setErpDocumentNumber(String erpDocumentNumber) {
        this.erpDocumentNumber = erpDocumentNumber;
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

    public Instant getPostedAt() {
        return postedAt;
    }

    public void setPostedAt(Instant postedAt) {
        this.postedAt = postedAt;
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
