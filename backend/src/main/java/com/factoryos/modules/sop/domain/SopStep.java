package com.factoryos.modules.sop.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sop_steps", uniqueConstraints = {
    @UniqueConstraint(name = "uq_sop_step_number", columnNames = {"sop_id", "step_number"})
})
public class SopStep {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sop_id", nullable = false)
    private StandardOperatingProcedure sop;

    @Column(name = "step_number", nullable = false)
    private int stepNumber;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "instruction_text", nullable = false, columnDefinition = "TEXT")
    private String instructionText;

    @Enumerated(EnumType.STRING)
    @Column(name = "step_type", nullable = false, length = 30)
    private SopStepType stepType = SopStepType.INSTRUCTION;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "cad_view_node", length = 100)
    private String cadViewNode;

    @Column(name = "is_mandatory", nullable = false)
    private boolean isMandatory = true;

    @Column(name = "nominal_value")
    private Double nominalValue;

    @Column(name = "min_tolerance")
    private Double minTolerance;

    @Column(name = "max_tolerance")
    private Double maxTolerance;

    @Column(name = "unit_of_measure", length = 32)
    private String unitOfMeasure;

    @Column(name = "safety_alert", length = 500)
    private String safetyAlert;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public SopStep() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public StandardOperatingProcedure getSop() {
        return sop;
    }

    public void setSop(StandardOperatingProcedure sop) {
        this.sop = sop;
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
