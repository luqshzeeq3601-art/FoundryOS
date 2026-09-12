package com.factoryos.modules.sop.dto;

import com.factoryos.modules.sop.domain.SopCategory;
import com.factoryos.modules.sop.domain.SopStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SopDto {

    private UUID id;
    private String sopCode;
    private String title;
    private String productCode;
    private UUID plantId;
    private String plantName;
    private String version;
    private SopCategory category;
    private SopStatus status;
    private String description;
    private String cadDrawingUrl;
    private String safetyPrecautions;
    private int estimatedDurationMinutes;
    private boolean requiresQualitySignOff;
    private List<SopStepDto> steps = new ArrayList<>();
    private Instant createdAt;
    private Instant updatedAt;

    public SopDto() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getSopCode() {
        return sopCode;
    }

    public void setSopCode(String sopCode) {
        this.sopCode = sopCode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public UUID getPlantId() {
        return plantId;
    }

    public void setPlantId(UUID plantId) {
        this.plantId = plantId;
    }

    public String getPlantName() {
        return plantName;
    }

    public void setPlantName(String plantName) {
        this.plantName = plantName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public SopCategory getCategory() {
        return category;
    }

    public void setCategory(SopCategory category) {
        this.category = category;
    }

    public SopStatus getStatus() {
        return status;
    }

    public void setStatus(SopStatus status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCadDrawingUrl() {
        return cadDrawingUrl;
    }

    public void setCadDrawingUrl(String cadDrawingUrl) {
        this.cadDrawingUrl = cadDrawingUrl;
    }

    public String getSafetyPrecautions() {
        return safetyPrecautions;
    }

    public void setSafetyPrecautions(String safetyPrecautions) {
        this.safetyPrecautions = safetyPrecautions;
    }

    public int getEstimatedDurationMinutes() {
        return estimatedDurationMinutes;
    }

    public void setEstimatedDurationMinutes(int estimatedDurationMinutes) {
        this.estimatedDurationMinutes = estimatedDurationMinutes;
    }

    public boolean isRequiresQualitySignOff() {
        return requiresQualitySignOff;
    }

    public void setRequiresQualitySignOff(boolean requiresQualitySignOff) {
        this.requiresQualitySignOff = requiresQualitySignOff;
    }

    public List<SopStepDto> getSteps() {
        return steps;
    }

    public void setSteps(List<SopStepDto> steps) {
        this.steps = steps != null ? steps : new ArrayList<>();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
