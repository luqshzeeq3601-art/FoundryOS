package com.factoryos.modules.sop.dto;

import com.factoryos.modules.sop.domain.SopStepType;
import com.factoryos.modules.sop.domain.StepRecordStatus;
import java.time.Instant;
import java.util.UUID;

public class SopStepExecutionRecordDto {

    private UUID id;
    private UUID sessionId;
    private UUID stepId;
    private int stepNumber;
    private String stepTitle;
    private String instructionText;
    private SopStepType stepType;
    private String imageUrl;
    private String cadViewNode;
    private boolean isMandatory;
    private Double nominalValue;
    private Double minTolerance;
    private Double maxTolerance;
    private String unitOfMeasure;
    private String safetyAlert;

    private StepRecordStatus status;
    private Double numericValue;
    private Boolean isWithinTolerance;
    private String textFeedback;
    private String photoEvidenceUrl;
    private String barcodeScanned;
    private UUID verifiedByUserId;
    private String verifiedByName;
    private Instant verifiedAt;
    private String notes;
    private Instant createdAt;

    public SopStepExecutionRecordDto() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    public UUID getStepId() {
        return stepId;
    }

    public void setStepId(UUID stepId) {
        this.stepId = stepId;
    }

    public int getStepNumber() {
        return stepNumber;
    }

    public void setStepNumber(int stepNumber) {
        this.stepNumber = stepNumber;
    }

    public String getStepTitle() {
        return stepTitle;
    }

    public void setStepTitle(String stepTitle) {
        this.stepTitle = stepTitle;
    }

    public String getInstructionText() {
        return instructionText;
    }

    public void setInstructionText(String instructionText) {
        this.instructionText = instructionText;
    }

    public SopStepType getStepType() {
        return stepType;
    }

    public void setStepType(SopStepType stepType) {
        this.stepType = stepType;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getCadViewNode() {
        return cadViewNode;
    }

    public void setCadViewNode(String cadViewNode) {
        this.cadViewNode = cadViewNode;
    }

    public boolean isMandatory() {
        return isMandatory;
    }

    public void setMandatory(boolean mandatory) {
        isMandatory = mandatory;
    }

    public Double getNominalValue() {
        return nominalValue;
    }

    public void setNominalValue(Double nominalValue) {
        this.nominalValue = nominalValue;
    }

    public Double getMinTolerance() {
        return minTolerance;
    }

    public void setMinTolerance(Double minTolerance) {
        this.minTolerance = minTolerance;
    }

    public Double getMaxTolerance() {
        return maxTolerance;
    }

    public void setMaxTolerance(Double maxTolerance) {
        this.maxTolerance = maxTolerance;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public String getSafetyAlert() {
        return safetyAlert;
    }

    public void setSafetyAlert(String safetyAlert) {
        this.safetyAlert = safetyAlert;
    }

    public StepRecordStatus getStatus() {
        return status;
    }

    public void setStatus(StepRecordStatus status) {
        this.status = status;
    }

    public Double getNumericValue() {
        return numericValue;
    }

    public void setNumericValue(Double numericValue) {
        this.numericValue = numericValue;
    }

    public Boolean getIsWithinTolerance() {
        return isWithinTolerance;
    }

    public void setIsWithinTolerance(Boolean withinTolerance) {
        isWithinTolerance = withinTolerance;
    }

    public String getTextFeedback() {
        return textFeedback;
    }

    public void setTextFeedback(String textFeedback) {
        this.textFeedback = textFeedback;
    }

    public String getPhotoEvidenceUrl() {
        return photoEvidenceUrl;
    }

    public void setPhotoEvidenceUrl(String photoEvidenceUrl) {
        this.photoEvidenceUrl = photoEvidenceUrl;
    }

    public String getBarcodeScanned() {
        return barcodeScanned;
    }

    public void setBarcodeScanned(String barcodeScanned) {
        this.barcodeScanned = barcodeScanned;
    }

    public UUID getVerifiedByUserId() {
        return verifiedByUserId;
    }

    public void setVerifiedByUserId(UUID verifiedByUserId) {
        this.verifiedByUserId = verifiedByUserId;
    }

    public String getVerifiedByName() {
        return verifiedByName;
    }

    public void setVerifiedByName(String verifiedByName) {
        this.verifiedByName = verifiedByName;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(Instant verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
