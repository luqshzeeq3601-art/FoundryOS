package com.factoryos.modules.barcode.dto;

import com.factoryos.modules.barcode.domain.BarcodeType;
import com.factoryos.modules.barcode.domain.BarcodeValidationStatus;
import com.factoryos.modules.barcode.domain.ResolvedEntityType;

import java.time.Instant;
import java.util.UUID;

public class BarcodeScanResponse {

    private UUID scanLogId;
    private String rawPayload;
    private BarcodeType barcodeType;
    private BarcodeValidationStatus validationStatus;
    private ResolvedEntityType resolvedEntityType;
    private String resolvedEntityId;
    private String resolvedEntitySummary;
    private boolean bomMatched;
    private String message;
    private long executionLatencyMs;
    private Instant timestamp;
    private Object entityData;

    public BarcodeScanResponse() {
    }

    public static BarcodeScanResponse valid(UUID scanLogId, String rawPayload, BarcodeType type, ResolvedEntityType entityType, String entityId, String summary, boolean bomMatched, String message, long latencyMs, Object entityData) {
        BarcodeScanResponse res = new BarcodeScanResponse();
        res.scanLogId = scanLogId;
        res.rawPayload = rawPayload;
        res.barcodeType = type;
        res.validationStatus = BarcodeValidationStatus.VALID;
        res.resolvedEntityType = entityType;
        res.resolvedEntityId = entityId;
        res.resolvedEntitySummary = summary;
        res.bomMatched = bomMatched;
        res.message = message;
        res.executionLatencyMs = latencyMs;
        res.timestamp = Instant.now();
        res.entityData = entityData;
        return res;
    }

    public static BarcodeScanResponse invalid(UUID scanLogId, String rawPayload, BarcodeType type, BarcodeValidationStatus status, ResolvedEntityType entityType, String entityId, String summary, String errorMessage, long latencyMs) {
        BarcodeScanResponse res = new BarcodeScanResponse();
        res.scanLogId = scanLogId;
        res.rawPayload = rawPayload;
        res.barcodeType = type;
        res.validationStatus = status;
        res.resolvedEntityType = entityType;
        res.resolvedEntityId = entityId;
        res.resolvedEntitySummary = summary;
        res.bomMatched = false;
        res.message = errorMessage;
        res.executionLatencyMs = latencyMs;
        res.timestamp = Instant.now();
        return res;
    }

    public UUID getScanLogId() {
        return scanLogId;
    }

    public void setScanLogId(UUID scanLogId) {
        this.scanLogId = scanLogId;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public void setRawPayload(String rawPayload) {
        this.rawPayload = rawPayload;
    }

    public BarcodeType getBarcodeType() {
        return barcodeType;
    }

    public void setBarcodeType(BarcodeType barcodeType) {
        this.barcodeType = barcodeType;
    }

    public BarcodeValidationStatus getValidationStatus() {
        return validationStatus;
    }

    public void setValidationStatus(BarcodeValidationStatus validationStatus) {
        this.validationStatus = validationStatus;
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

    public boolean isBomMatched() {
        return bomMatched;
    }

    public void setBomMatched(boolean bomMatched) {
        this.bomMatched = bomMatched;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getExecutionLatencyMs() {
        return executionLatencyMs;
    }

    public void setExecutionLatencyMs(long executionLatencyMs) {
        this.executionLatencyMs = executionLatencyMs;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Object getEntityData() {
        return entityData;
    }

    public void setEntityData(Object entityData) {
        this.entityData = entityData;
    }
}
