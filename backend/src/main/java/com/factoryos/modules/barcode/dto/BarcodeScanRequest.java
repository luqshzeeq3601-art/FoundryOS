package com.factoryos.modules.barcode.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public class BarcodeScanRequest {

    @NotBlank(message = "Scan payload cannot be blank")
    private String rawPayload;

    private String barcodeFormat; // QR_CODE, DATA_MATRIX, CODE_128, PDF_417, UNKNOWN
    private String scannerSource; // HARDWARE_WEDGE, CAMERA_ZXING, MANUAL_KEYPAD
    private UUID machineId;
    private UUID productionOrderId;

    public BarcodeScanRequest() {
    }

    public BarcodeScanRequest(String rawPayload, String barcodeFormat, String scannerSource, UUID machineId, UUID productionOrderId) {
        this.rawPayload = rawPayload;
        this.barcodeFormat = barcodeFormat;
        this.scannerSource = scannerSource;
        this.machineId = machineId;
        this.productionOrderId = productionOrderId;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public void setRawPayload(String rawPayload) {
        this.rawPayload = rawPayload;
    }

    public String getBarcodeFormat() {
        return barcodeFormat;
    }

    public void setBarcodeFormat(String barcodeFormat) {
        this.barcodeFormat = barcodeFormat;
    }

    public String getScannerSource() {
        return scannerSource;
    }

    public void setScannerSource(String scannerSource) {
        this.scannerSource = scannerSource;
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
}
