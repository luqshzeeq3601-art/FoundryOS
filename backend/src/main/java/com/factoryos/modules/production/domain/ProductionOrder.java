package com.factoryos.modules.production.domain;

import com.factoryos.modules.machine.domain.Machine;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "production_orders")
public class ProductionOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "order_number", nullable = false, unique = true, length = 40)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id", nullable = false)
    private Machine machine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plant_id")
    private com.factoryos.modules.tenant.domain.Plant plant;

    @Column(name = "product_code", nullable = false, length = 100)
    private String productCode;

    @Column(name = "product_description", length = 500)
    private String productDescription;

    @Column(name = "planned_quantity", nullable = false)
    private int plannedQuantity;

    @Column(name = "good_quantity", nullable = false)
    private int goodQuantity = 0;

    @Column(name = "scrap_quantity", nullable = false)
    private int scrapQuantity = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ProductionOrderStatus status = ProductionOrderStatus.DRAFT;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "closure_note", length = 2000)
    private String closureNote;

    @Column(name = "erp_system", length = 32)
    private String erpSystem;

    @Column(name = "erp_order_id", length = 64)
    private String erpOrderId;

    @Column(name = "erp_batch_number", length = 64)
    private String erpBatchNumber;

    @Column(name = "erp_sync_status", nullable = false, length = 32)
    private String erpSyncStatus = "LOCAL_ONLY";

    @Column(name = "last_erp_sync_at")
    private Instant lastErpSyncAt;

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

    public ProductionOrder() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public Machine getMachine() {
        return machine;
    }

    public void setMachine(Machine machine) {
        this.machine = machine;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getProductDescription() {
        return productDescription;
    }

    public void setProductDescription(String productDescription) {
        this.productDescription = productDescription;
    }

    public int getPlannedQuantity() {
        return plannedQuantity;
    }

    public void setPlannedQuantity(int plannedQuantity) {
        this.plannedQuantity = plannedQuantity;
    }

    public int getGoodQuantity() {
        return goodQuantity;
    }

    public void setGoodQuantity(int goodQuantity) {
        this.goodQuantity = goodQuantity;
    }

    public int getScrapQuantity() {
        return scrapQuantity;
    }

    public void setScrapQuantity(int scrapQuantity) {
        this.scrapQuantity = scrapQuantity;
    }

    public ProductionOrderStatus getStatus() {
        return status;
    }

    public void setStatus(ProductionOrderStatus status) {
        this.status = status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    public String getClosureNote() {
        return closureNote;
    }

    public void setClosureNote(String closureNote) {
        this.closureNote = closureNote;
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

    public com.factoryos.modules.tenant.domain.Plant getPlant() {
        return plant;
    }

    public void setPlant(com.factoryos.modules.tenant.domain.Plant plant) {
        this.plant = plant;
    }

    public String getErpSystem() {
        return erpSystem;
    }

    public void setErpSystem(String erpSystem) {
        this.erpSystem = erpSystem;
    }

    public String getErpOrderId() {
        return erpOrderId;
    }

    public void setErpOrderId(String erpOrderId) {
        this.erpOrderId = erpOrderId;
    }

    public String getErpBatchNumber() {
        return erpBatchNumber;
    }

    public void setErpBatchNumber(String erpBatchNumber) {
        this.erpBatchNumber = erpBatchNumber;
    }

    public String getErpSyncStatus() {
        return erpSyncStatus;
    }

    public void setErpSyncStatus(String erpSyncStatus) {
        this.erpSyncStatus = erpSyncStatus;
    }

    public Instant getLastErpSyncAt() {
        return lastErpSyncAt;
    }

    public void setLastErpSyncAt(Instant lastErpSyncAt) {
        this.lastErpSyncAt = lastErpSyncAt;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }
}
