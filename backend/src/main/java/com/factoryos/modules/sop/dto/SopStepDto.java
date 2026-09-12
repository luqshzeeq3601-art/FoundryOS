package com.factoryos.modules.sop.dto;

import com.factoryos.modules.sop.domain.SopStepType;
import java.util.UUID;

public class SopStepDto {

    private UUID id;
    private UUID sopId;
    private int stepNumber;
    private String title;
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

    public SopStepDto() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getSopId() {
        return sopId;
    }

    public void setSopId(UUID sopId) {
        this.sopId = sopId;
    }

    public int getStepNumber() {
        return stepNumber;
    }

    public void setStepNumber(int stepNumber) {
        this.stepNumber = stepNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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
}
