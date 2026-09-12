package com.factoryos.modules.edge.dto;

import java.util.UUID;

public class SimulateDisconnectRequestDto {

    private String gatewayCode;
    private double disconnectDurationHours = 4.0;
    private UUID orderId;
    private int producedPartsGood = 1200;
    private int producedPartsScrap = 15;
    private int barcodeScans = 25;
    private int downtimeDurationMinutes = 15;
    private String downtimeReason = "Feeder Jam";
    private UUID machineId;

    public SimulateDisconnectRequestDto() {
    }

    public String getGatewayCode() {
        return gatewayCode;
    }

    public void setGatewayCode(String gatewayCode) {
        this.gatewayCode = gatewayCode;
    }

    public double getDisconnectDurationHours() {
        return disconnectDurationHours;
    }

    public void setDisconnectDurationHours(double disconnectDurationHours) {
        this.disconnectDurationHours = disconnectDurationHours;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public int getProducedPartsGood() {
        return producedPartsGood;
    }

    public void setProducedPartsGood(int producedPartsGood) {
        this.producedPartsGood = producedPartsGood;
    }

    public int getProducedPartsScrap() {
        return producedPartsScrap;
    }

    public void setProducedPartsScrap(int producedPartsScrap) {
        this.producedPartsScrap = producedPartsScrap;
    }

    public int getBarcodeScans() {
        return barcodeScans;
    }

    public void setBarcodeScans(int barcodeScans) {
        this.barcodeScans = barcodeScans;
    }

    public int getDowntimeDurationMinutes() {
        return downtimeDurationMinutes;
    }

    public void setDowntimeDurationMinutes(int downtimeDurationMinutes) {
        this.downtimeDurationMinutes = downtimeDurationMinutes;
    }

    public String getDowntimeReason() {
        return downtimeReason;
    }

    public void setDowntimeReason(String downtimeReason) {
        this.downtimeReason = downtimeReason;
    }

    public UUID getMachineId() {
        return machineId;
    }

    public void setMachineId(UUID machineId) {
        this.machineId = machineId;
    }
}
