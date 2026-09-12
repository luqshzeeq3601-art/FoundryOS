package com.factoryos.modules.erp.dto;

import com.factoryos.modules.erp.domain.ErpOrderConfirmation;

import java.time.Instant;
import java.util.UUID;

public class ErpOrderConfirmationDto {

    private UUID id;
    private UUID productionOrderId;
    private String orderNumber;
    private String erpOrderId;
    private String confirmationNumber;
    private int confirmedGoodQty;
    private int confirmedScrapQty;
    private String scrapReason;
    private double laborHours;
    private double machineHours;
    private String erpPostingStatus;
    private String erpDocumentNumber;
    private String errorMessage;
    private int retryCount;
    private Instant postedAt;
    private Instant createdAt;

    public ErpOrderConfirmationDto() {
    }

    public static ErpOrderConfirmationDto from(ErpOrderConfirmation c) {
        ErpOrderConfirmationDto dto = new ErpOrderConfirmationDto();
        dto.setId(c.getId());
        dto.setProductionOrderId(c.getProductionOrder().getId());
        dto.setOrderNumber(c.getProductionOrder().getOrderNumber());
        dto.setErpOrderId(c.getErpOrderId());
        dto.setConfirmationNumber(c.getConfirmationNumber());
        dto.setConfirmedGoodQty(c.getConfirmedGoodQty());
        dto.setConfirmedScrapQty(c.getConfirmedScrapQty());
        dto.setScrapReason(c.getScrapReason());
        dto.setLaborHours(c.getLaborHours());
        dto.setMachineHours(c.getMachineHours());
        dto.setErpPostingStatus(c.getErpPostingStatus());
        dto.setErpDocumentNumber(c.getErpDocumentNumber());
        dto.setErrorMessage(c.getErrorMessage());
        dto.setRetryCount(c.getRetryCount());
        dto.setPostedAt(c.getPostedAt());
        dto.setCreatedAt(c.getCreatedAt());
        return dto;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getProductionOrderId() {
        return productionOrderId;
    }

    public void setProductionOrderId(UUID productionOrderId) {
        this.productionOrderId = productionOrderId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getErpOrderId() {
        return erpOrderId;
    }

    public void setErpOrderId(String erpOrderId) {
        this.erpOrderId = erpOrderId;
    }

    public String getConfirmationNumber() {
        return confirmationNumber;
    }

    public void setConfirmationNumber(String confirmationNumber) {
        this.confirmationNumber = confirmationNumber;
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
}