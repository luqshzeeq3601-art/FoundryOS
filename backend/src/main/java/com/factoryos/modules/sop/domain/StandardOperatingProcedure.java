package com.factoryos.modules.sop.domain;

import com.factoryos.modules.tenant.domain.Plant;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "standard_operating_procedures")
public class StandardOperatingProcedure {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "sop_code", nullable = false, unique = true, length = 50)
    private String sopCode;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "product_code", nullable = false, length = 100)
    private String productCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plant_id")
    private Plant plant;

    @Column(nullable = false, length = 20)
    private String version = "1.0";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private SopCategory category = SopCategory.ASSEMBLY;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SopStatus status = SopStatus.PUBLISHED;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "cad_drawing_url", length = 500)
    private String cadDrawingUrl;

    @Column(name = "safety_precautions", columnDefinition = "TEXT")
    private String safetyPrecautions;

    @Column(name = "estimated_duration_minutes", nullable = false)
    private int estimatedDurationMinutes = 30;

    @Column(name = "requires_quality_sign_off", nullable = false)
    private boolean requiresQualitySignOff = true;

    @OneToMany(mappedBy = "sop", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("stepNumber ASC")
    private List<SopStep> steps = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Version
    @Column(name = "version_lock", nullable = false)
    private long versionLock = 0L;

    public StandardOperatingProcedure() {
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
        this.sopCode = sopCode != null ? sopCode.trim().toUpperCase() : null;
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
        this.productCode = productCode != null ? productCode.trim().toUpperCase() : null;
    }

    public Plant getPlant() {
        return plant;
    }

    public void setPlant(Plant plant) {
        this.plant = plant;
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

    public List<SopStep> getSteps() {
        return steps;
    }

    public void setSteps(List<SopStep> steps) {
        this.steps = steps != null ? steps : new ArrayList<>();
    }

    public void addStep(SopStep step) {
        this.steps.add(step);
        step.setSop(this);
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

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public long getVersionLock() {
        return versionLock;
    }

    public void setVersionLock(long versionLock) {
        this.versionLock = versionLock;
    }
}
