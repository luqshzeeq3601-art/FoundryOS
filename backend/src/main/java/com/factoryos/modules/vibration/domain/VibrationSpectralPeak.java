package com.factoryos.modules.vibration.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "vibration_spectral_peaks")
public class VibrationSpectralPeak {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "burst_id", nullable = false)
    private VibrationBurstSample burst;

    @Column(name = "frequency_hz", nullable = false)
    private double frequencyHz;

    @Column(name = "amplitude_mm_s", nullable = false)
    private double amplitudeMmS;

    @Column(name = "order_multiple")
    private Double orderMultiple;

    @Enumerated(EnumType.STRING)
    @Column(name = "fault_harmonic_type", nullable = false, length = 64)
    private FaultHarmonicType faultHarmonicType = FaultHarmonicType.NORMAL;

    @Column(nullable = false)
    private double confidence = 1.0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public VibrationSpectralPeak() {
    }

    public VibrationSpectralPeak(double frequencyHz, double amplitudeMmS, Double orderMultiple, FaultHarmonicType faultHarmonicType, double confidence) {
        this.frequencyHz = frequencyHz;
        this.amplitudeMmS = amplitudeMmS;
        this.orderMultiple = orderMultiple;
        this.faultHarmonicType = faultHarmonicType;
        this.confidence = confidence;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public VibrationBurstSample getBurst() {
        return burst;
    }

    public void setBurst(VibrationBurstSample burst) {
        this.burst = burst;
    }

    public double getFrequencyHz() {
        return frequencyHz;
    }

    public void setFrequencyHz(double frequencyHz) {
        this.frequencyHz = frequencyHz;
    }

    public double getAmplitudeMmS() {
        return amplitudeMmS;
    }

    public void setAmplitudeMmS(double amplitudeMmS) {
        this.amplitudeMmS = amplitudeMmS;
    }

    public Double getOrderMultiple() {
        return orderMultiple;
    }

    public void setOrderMultiple(Double orderMultiple) {
        this.orderMultiple = orderMultiple;
    }

    public FaultHarmonicType getFaultHarmonicType() {
        return faultHarmonicType;
    }

    public void setFaultHarmonicType(FaultHarmonicType faultHarmonicType) {
        this.faultHarmonicType = faultHarmonicType;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
