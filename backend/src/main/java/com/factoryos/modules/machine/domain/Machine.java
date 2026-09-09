package com.factoryos.modules.machine.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "machines")
public class Machine {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "serial_number", nullable = false, unique = true, length = 80)
    private String serialNumber;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 160)
    private String location;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MachineStatus status = MachineStatus.IDLE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plant_id")
    private com.factoryos.modules.tenant.domain.Plant plant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_id")
    private com.factoryos.modules.tenant.domain.ProductionArea area;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "line_id")
    private com.factoryos.modules.tenant.domain.ProductionLine line;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_cell_id")
    private com.factoryos.modules.tenant.domain.WorkCell workCell;

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

    public Machine() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber != null ? serialNumber.trim().toUpperCase() : null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public MachineStatus getStatus() {
        return status;
    }

    public void setStatus(MachineStatus status) {
        this.status = status;
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

    public com.factoryos.modules.tenant.domain.ProductionArea getArea() {
        return area;
    }

    public void setArea(com.factoryos.modules.tenant.domain.ProductionArea area) {
        this.area = area;
    }

    public com.factoryos.modules.tenant.domain.ProductionLine getLine() {
        return line;
    }

    public void setLine(com.factoryos.modules.tenant.domain.ProductionLine line) {
        this.line = line;
    }

    public com.factoryos.modules.tenant.domain.WorkCell getWorkCell() {
        return workCell;
    }

    public void setWorkCell(com.factoryos.modules.tenant.domain.WorkCell workCell) {
        this.workCell = workCell;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }
}
