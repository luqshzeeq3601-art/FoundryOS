package com.factoryos.modules.sop.dto;

import com.factoryos.modules.sop.domain.StepRecordStatus;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class RecordStepExecutionRequestDto {

    @NotNull(message = "Step ID is required")
    private UUID stepId;

    @NotNull(message = "Step Status is required")
    private StepRecordStatus status;

    private Double numericValue;
    private String textFeedback;
    private String photoEvidenceUrl;
    private String barcodeScanned;
    private String notes;

    public RecordStepExecutionRequestDto() {
    }

    public UUID getStepId() {
        return stepId;
    }

    public void setStepId(UUID stepId) {
        this.stepId = stepId;
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
