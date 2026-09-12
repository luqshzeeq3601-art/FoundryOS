package com.factoryos.modules.sop.dto;

import com.factoryos.modules.sop.domain.SopSessionStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SopExecutionSessionDto {

    private UUID id;
    private UUID sopId;
    private String sopCode;
    private String sopTitle;
    private String cadDrawingUrl;
    private String safetyPrecautions;
    private UUID productionOrderId;
    private String orderNumber;
    private String productCode;
    private UUID machineId;
    private String machineCode;
    private UUID plantId;
    private String plantName;
    private SopSessionStatus sessionStatus;
    private Instant startedAt;
    private Instant completedAt;
    private UUID operatorUserId;
    private String operatorName;
    private UUID qualitySignOffBy;
    private String qualitySignOffName;
    private Instant qualitySignOffAt;
    private String qualitySignOffNotes;
    private int totalSteps;
    private int completedSteps;
    private int passedSteps;
    private int failedSteps;
    private List<SopStepExecutionRecordDto> stepRecords = new ArrayList<>();

    public SopExecutionSessionDto() {
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

    public UUID getMachineId() {
        return machineId;
    }

    public void setMachineId(UUID machineId) {
        this.machineId = machineId;
    }

    public String getMachineCode() {
        return machineCode;
    }

    public void setMachineCode(String machineCode) {
        this.machineCode = machineCode;
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

    public SopSessionStatus getSessionStatus() {
        return sessionStatus;
    }

    public void setSessionStatus(SopSessionStatus sessionStatus) {
        this.sessionStatus = sessionStatus;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public UUID getOperatorUserId() {
        return operatorUserId;
    }

    public void setOperatorUserId(UUID operatorUserId) {
        this.operatorUserId = operatorUserId;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public UUID getQualitySignOffBy() {
        return qualitySignOffBy;
    }

    public void setQualitySignOffBy(UUID qualitySignOffBy) {
        this.qualitySignOffBy = qualitySignOffBy;
    }

    public String getQualitySignOffName() {
        return qualitySignOffName;
    }

    public void setQualitySignOffName(String qualitySignOffName) {
        this.qualitySignOffName = qualitySignOffName;
    }

    public Instant getQualitySignOffAt() {
        return qualitySignOffAt;
    }

    public void setQualitySignOffAt(Instant qualitySignOffAt) {
        this.qualitySignOffAt = qualitySignOffAt;
    }

    public String getQualitySignOffNotes() {
        return qualitySignOffNotes;
    }

    public void setQualitySignOffNotes(String qualitySignOffNotes) {
        this.qualitySignOffNotes = qualitySignOffNotes;
    }

    public int getTotalSteps() {
        return totalSteps;
    }

    public void setTotalSteps(int totalSteps) {
        this.totalSteps = totalSteps;
    }

    public int getCompletedSteps() {
        return completedSteps;
    }

    public void setCompletedSteps(int completedSteps) {
        this.completedSteps = completedSteps;
    }

    public int getPassedSteps() {
        return passedSteps;
    }

    public void setPassedSteps(int passedSteps) {
        this.passedSteps = passedSteps;
    }

    public int getFailedSteps() {
        return failedSteps;
    }

    public void setFailedSteps(int failedSteps) {
        this.failedSteps = failedSteps;
    }

    public List<SopStepExecutionRecordDto> getStepRecords() {
        return stepRecords;
    }

    public void setStepRecords(List<SopStepExecutionRecordDto> stepRecords) {
        this.stepRecords = stepRecords != null ? stepRecords : new ArrayList<>();
    }
}
