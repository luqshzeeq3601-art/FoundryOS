package com.factoryos.modules.sop.domain;

import com.factoryos.modules.auth.domain.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sop_step_execution_records", uniqueConstraints = {
    @UniqueConstraint(name = "uq_session_step", columnNames = {"session_id", "step_id"})
})
public class SopStepExecutionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private SopExecutionSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "step_id", nullable = false)
    private SopStep step;

    @Column(name = "step_number", nullable = false)
    private int stepNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StepRecordStatus status = StepRecordStatus.PENDING;

    @Column(name = "numeric_value")
    private Double numericValue;

    @Column(name = "is_within_tolerance")
    private Boolean isWithinTolerance;

    @Column(name = "text_feedback", length = 1000)
    private String textFeedback;

    @Column(name = "photo_evidence_url", length = 1000)
    private String photoEvidenceUrl;

    @Column(name = "barcode_scanned", length = 100)
    private String barcodeScanned;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by_user_id")
    private User verifiedByUser;

    @Column(name = "verified_by_name", length = 100)
    private String verifiedByName;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(length = 1000)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public SopStepExecutionRecord() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public SopExecutionSession getSession() {
        return session;
    }

    public void setSession(SopExecutionSession session) {
        this.session = session;
    }

    public SopStep getStep() {
        return step;
    }

    public void setStep(SopStep step) {
        this.step = step;
    }

    public int getStepNumber() {
        return stepNumber;
    }

    public void setStepNumber(int stepNumber) {
        this.stepNumber = stepNumber;
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

    public User getVerifiedByUser() {
        return verifiedByUser;
    }

    public void setVerifiedByUser(User verifiedByUser) {
        this.verifiedByUser = verifiedByUser;
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
