package com.factoryos.modules.barcode.dto;

import com.factoryos.modules.barcode.domain.BarcodeType;
import com.factoryos.modules.barcode.domain.BarcodeValidationStatus;
import com.factoryos.modules.barcode.domain.ResolvedEntityType;

import java.time.Instant;
import java.util.UUID;

public class BarcodeScanLogDto {

    private UUID id;
    private String scanPayload;
    private String barcodeFormat;
    private BarcodeType barcodeType;
    private String scannerSource;
    private ResolvedEntityType resolvedEntityType;
    private String resolvedEntityId;
    private String resolvedEntitySummary;
    private UUID machineId;
    private UUID productionOrderId;
    private BarcodeValidationStatus validationStatus;
    private boolean bomMatched;
    private String errorMessage;
    private UUID scannedByUserId;
    private String scannedByName;
    private long latencyMs;
    private Instant createdAt;

    public BarcodeScanLogDto() {
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

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
