package com.factoryos.modules.sop.domain;

import com.factoryos.modules.auth.domain.User;
import com.factoryos.modules.machine.domain.Machine;
import com.factoryos.modules.production.domain.ProductionOrder;
import com.factoryos.modules.tenant.domain.Plant;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "sop_execution_sessions")
public class SopExecutionSession {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sop_id", nullable = false)
    private StandardOperatingProcedure sop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "production_order_id", nullable = false)
    private ProductionOrder productionOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id")
    private Machine machine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plant_id", nullable = false)
    private Plant plant;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_status", nullable = false, length = 25)
    private SopSessionStatus sessionStatus = SopSessionStatus.IN_PROGRESS;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_user_id")
    private User operatorUser;

    @Column(name = "operator_name", length = 100)
    private String operatorName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quality_sign_off_by")
    private User qualitySignOffBy;

    @Column(name = "quality_sign_off_name", length = 100)
    private String qualitySignOffName;

    @Column(name = "quality_sign_off_at")
    private Instant qualitySignOffAt;

    @Column(name = "quality_sign_off_notes", columnDefinition = "TEXT")
    private String qualitySignOffNotes;

    @Column(name = "total_steps", nullable = false)
    private int totalSteps = 0;

    @Column(name = "completed_steps", nullable = false)
    private int completedSteps = 0;

    @Column(name = "passed_steps", nullable = false)
    private int passedSteps = 0;

    @Column(name = "failed_steps", nullable = false)
    private int failedSteps = 0;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("stepNumber ASC")
    private List<SopStepExecutionRecord> stepRecords = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public SopExecutionSession() {
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

    public ProductionOrder getProductionOrder() {
        return productionOrder;
    }

    public void setProductionOrder(ProductionOrder productionOrder) {
        this.productionOrder = productionOrder;
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

    public User getOperatorUser() {
        return operatorUser;
    }

    public void setOperatorUser(User operatorUser) {
        this.operatorUser = operatorUser;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public User getQualitySignOffBy() {
        return qualitySignOffBy;
    }

    public void setQualitySignOffBy(User qualitySignOffBy) {
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

    public List<SopStepExecutionRecord> getStepRecords() {
        return stepRecords;
    }

    public void setStepRecords(List<SopStepExecutionRecord> stepRecords) {
        this.stepRecords = stepRecords != null ? stepRecords : new ArrayList<>();
    }

    public void addStepRecord(SopStepExecutionRecord record) {
        this.stepRecords.add(record);
        record.setSession(this);
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
