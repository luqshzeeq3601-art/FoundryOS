package com.factoryos.modules.barcode.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "barcode_scan_logs")
public class BarcodeScanLog {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "scan_payload", nullable = false, length = 1000)
    private String scanPayload;

    @Column(name = "barcode_format", nullable = false, length = 64)
    private String barcodeFormat = "UNKNOWN";

    @Enumerated(EnumType.STRING)
    @Column(name = "barcode_type", nullable = false, length = 64)
    private BarcodeType barcodeType = BarcodeType.UNKNOWN;

    @Column(name = "scanner_source", nullable = false, length = 64)
    private String scannerSource = "HARDWARE_WEDGE";

    @Enumerated(EnumType.STRING)
    @Column(name = "resolved_entity_type", nullable = false, length = 64)
    private ResolvedEntityType resolvedEntityType = ResolvedEntityType.NONE;

    @Column(name = "resolved_entity_id", length = 128)
    private String resolvedEntityId;

    @Column(name = "resolved_entity_summary", length = 500)
    private String resolvedEntitySummary;

    @Column(name = "machine_id")
    private UUID machineId;

    @Column(name = "production_order_id")
    private UUID productionOrderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "validation_status", nullable = false, length = 64)
    private BarcodeValidationStatus validationStatus = BarcodeValidationStatus.VALID;

    @Column(name = "bom_matched", nullable = false)
    private boolean bomMatched = true;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "scanned_by_user_id")
    private UUID scannedByUserId;

    @Column(name = "scanned_by_name", length = 255)
    private String scannedByName;

    @Column(name = "latency_ms", nullable = false)
    private long latencyMs = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public BarcodeScanLog() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getScanPayload() {
        return scanPayload;
    }

    public void setScanPayload(String scanPayload) {
        this.scanPayload = scanPayload;
    }

    public String getBarcodeFormat() {
        return barcodeFormat;
    }

    public void setBarcodeFormat(String barcodeFormat) {
        this.barcodeFormat = barcodeFormat;
    }

    public BarcodeType getBarcodeType() {
        return barcodeType;
    }

    public void setBarcodeType(BarcodeType barcodeType) {
        this.barcodeType = barcodeType;
    }

    public String getScannerSource() {
        return scannerSource;
    }

    public void setScannerSource(String scannerSource) {
        this.scannerSource = scannerSource;
    }

    public ResolvedEntityType getResolvedEntityType() {
        return resolvedEntityType;
    }

    public void setResolvedEntityType(ResolvedEntityType resolvedEntityType) {
        this.resolvedEntityType = resolvedEntityType;
    }

    public String getResolvedEntityId() {
        return resolvedEntityId;
    }

    public void setResolvedEntityId(String resolvedEntityId) {
        this.resolvedEntityId = resolvedEntityId;
    }

    public String getResolvedEntitySummary() {
        return resolvedEntitySummary;
    }

    public void setResolvedEntitySummary(String resolvedEntitySummary) {
        this.resolvedEntitySummary = resolvedEntitySummary;
    }

    public UUID getMachineId() {
        return machineId;
    }

    public void setMachineId(UUID machineId) {
        this.machineId = machineId;
    }

    public UUID getProductionOrderId() {
        return productionOrderId;
    }

    public void setProductionOrderId(UUID productionOrderId) {
        this.productionOrderId = productionOrderId;
    }

    public BarcodeValidationStatus getValidationStatus() {
        return validationStatus;
    }

    public void setValidationStatus(BarcodeValidationStatus validationStatus) {
        this.validationStatus = validationStatus;
    }

    public boolean isBomMatched() {
        return bomMatched;
    }

    public void setBomMatched(boolean bomMatched) {
        this.bomMatched = bomMatched;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public UUID getScannedByUserId() {
        return scannedByUserId;
    }

    public void setScannedByUserId(UUID scannedByUserId) {
        this.scannedByUserId = scannedByUserId;
    }

    public String getScannedByName() {
        return scannedByName;
    }

    public void setScannedByName(String scannedByName) {
        this.scannedByName = scannedByName;
    }

    public long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
