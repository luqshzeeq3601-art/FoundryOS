package com.factoryos.modules.sop.dto;

import com.factoryos.modules.sop.domain.GateStatus;
import java.time.Instant;
import java.util.UUID;

public class QualityGateStatusDto {

    private UUID gateId;
    private UUID productionOrderId;
    private String orderNumber;
    private String productCode;
    private UUID sopId;
    private String sopCode;
    private String sopTitle;
    private UUID sessionId;
    private GateStatus gateStatus;
    private boolean isCompliant;
    private boolean requiresQualityRole;
    private int totalMandatorySteps;
    private int completedMandatorySteps;
    private int failedMandatorySteps;
    private UUID signedOffByUserId;
    private String signedOffByName;
    private Instant signedOffAt;
    private String signOffComments;
    private String blockingReason;

    public QualityGateStatusDto() {
    }

    public UUID getGateId() {
        return gateId;
    }

    public void setGateId(UUID gateId) {
        this.gateId = gateId;
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

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public UUID getSopId() {
        return sopId;
    }

    public void setSopId(UUID sopId) {
        this.sopId = sopId;
    }

    public String getSopCode() {
        return sopCode;
    }

    public void setSopCode(String sopCode) {
        this.sopCode = sopCode;
    }

    public String getSopTitle() {
        return sopTitle;
    }

    public void setSopTitle(String sopTitle) {
        this.sopTitle = sopTitle;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    public GateStatus getGateStatus() {
        return gateStatus;
    }

    public void setGateStatus(GateStatus gateStatus) {
        this.gateStatus = gateStatus;
    }

    public boolean isCompliant() {
        return isCompliant;
    }

    public void setCompliant(boolean compliant) {
        isCompliant = compliant;
    }

    public boolean isRequiresQualityRole() {
        return requiresQualityRole;
    }

    public void setRequiresQualityRole(boolean requiresQualityRole) {
        this.requiresQualityRole = requiresQualityRole;
    }

    public int getTotalMandatorySteps() {
        return totalMandatorySteps;
    }

    public void setTotalMandatorySteps(int totalMandatorySteps) {
        this.totalMandatorySteps = totalMandatorySteps;
    }

    public int getCompletedMandatorySteps() {
        return completedMandatorySteps;
    }

    public void setCompletedMandatorySteps(int completedMandatorySteps) {
        this.completedMandatorySteps = completedMandatorySteps;
    }

    public int getFailedMandatorySteps() {
        return failedMandatorySteps;
    }

    public void setFailedMandatorySteps(int failedMandatorySteps) {
        this.failedMandatorySteps = failedMandatorySteps;
    }

    public UUID getSignedOffByUserId() {
        return signedOffByUserId;
    }

    public void setSignedOffByUserId(UUID signedOffByUserId) {
        this.signedOffByUserId = signedOffByUserId;
    }

    public String getSignedOffByName() {
        return signedOffByName;
    }

    public void setSignedOffByName(String signedOffByName) {
        this.signedOffByName = signedOffByName;
    }

    public Instant getSignedOffAt() {
        return signedOffAt;
    }

    public void setSignedOffAt(Instant signedOffAt) {
        this.signedOffAt = signedOffAt;
    }

    public String getSignOffComments() {
        return signOffComments;
    }

    public void setSignOffComments(String signOffComments) {
        this.signOffComments = signOffComments;
    }

    public String getBlockingReason() {
        return blockingReason;
    }

    public void setBlockingReason(String blockingReason) {
        this.blockingReason = blockingReason;
    }
}
