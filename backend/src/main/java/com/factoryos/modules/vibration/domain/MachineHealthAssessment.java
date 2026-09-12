package com.factoryos.modules.vibration.domain;

import com.factoryos.modules.machine.domain.Machine;
import com.factoryos.modules.tenant.domain.Plant;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "machine_health_assessments")
public class MachineHealthAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id", nullable = false)
    private Machine machine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plant_id")
    private Plant plant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "latest_burst_id")
    private VibrationBurstSample latestBurst;

    @Column(name = "health_score", nullable = false)
    private int healthScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "health_status", nullable = false, length = 32)
    private MachineHealthStatus healthStatus = MachineHealthStatus.GOOD;

    @Enumerated(EnumType.STRING)
    @Column(name = "iso_severity_zone", nullable = false, length = 16)
    private IsoSeverityZone isoSeverityZone = IsoSeverityZone.ZONE_A;

    @Enumerated(EnumType.STRING)
    @Column(name = "vibration_class", nullable = false, length = 32)
    private MachineVibrationClass vibrationClass = MachineVibrationClass.CLASS_II_MEDIUM;

    @Column(name = "rms_velocity_mm_s", nullable = false)
    private double rmsVelocityMmS;

    @Column(name = "spindle_temperature_c")
    private Double spindleTemperatureC;

    @Column(name = "dominant_fault_type", length = 64)
    private String dominantFaultType;

    @Column(name = "diagnosis_summary", nullable = false, columnDefinition = "TEXT")
    private String diagnosisSummary;

    @Column(name = "recommended_action", columnDefinition = "TEXT")
    private String recommendedAction;

    @Column(name = "assessed_at", nullable = false)
    private Instant assessedAt = Instant.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    public MachineHealthAssessment() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Machine getMachine() {
        return machine;
    }

    public void setMachine(Machine machine) {
        this.machine = machine;
    }

    public Plant getPlant() {
        return plant;
    }

    public void setPlant(Plant plant) {
        this.plant = plant;
    }

    public VibrationBurstSample getLatestBurst() {
        return latestBurst;
    }

    public void setLatestBurst(VibrationBurstSample latestBurst) {
        this.latestBurst = latestBurst;
    }

    public int getHealthScore() {
        return healthScore;
    }

    public void setHealthScore(int healthScore) {
        this.healthScore = healthScore;
    }

    public MachineHealthStatus getHealthStatus() {
        return healthStatus;
    }

    public void setHealthStatus(MachineHealthStatus healthStatus) {
        this.healthStatus = healthStatus;
    }

    public IsoSeverityZone getIsoSeverityZone() {
        return isoSeverityZone;
    }

    public void setIsoSeverityZone(IsoSeverityZone isoSeverityZone) {
        this.isoSeverityZone = isoSeverityZone;
    }

    public MachineVibrationClass getVibrationClass() {
        return vibrationClass;
    }

    public void setVibrationClass(MachineVibrationClass vibrationClass) {
        this.vibrationClass = vibrationClass;
    }

    public double getRmsVelocityMmS() {
        return rmsVelocityMmS;
    }

    public void setRmsVelocityMmS(double rmsVelocityMmS) {
        this.rmsVelocityMmS = rmsVelocityMmS;
    }

    public Double getSpindleTemperatureC() {
        return spindleTemperatureC;
    }

    public void setSpindleTemperatureC(Double spindleTemperatureC) {
        this.spindleTemperatureC = spindleTemperatureC;
    }

    public String getDominantFaultType() {
        return dominantFaultType;
    }

    public void setDominantFaultType(String dominantFaultType) {
        this.dominantFaultType = dominantFaultType;
    }

    public String getDiagnosisSummary() {
        return diagnosisSummary;
    }

    public void setDiagnosisSummary(String diagnosisSummary) {
        this.diagnosisSummary = diagnosisSummary;
    }

    public String getRecommendedAction() {
        return recommendedAction;
    }

    public void setRecommendedAction(String recommendedAction) {
        this.recommendedAction = recommendedAction;
    }

    public Instant getAssessedAt() {
        return assessedAt;
    }

    public void setAssessedAt(Instant assessedAt) {
        this.assessedAt = assessedAt;
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

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        this.isDeleted = deleted;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
