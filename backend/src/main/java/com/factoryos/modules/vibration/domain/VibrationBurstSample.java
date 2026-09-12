package com.factoryos.modules.vibration.domain;

import com.factoryos.modules.machine.domain.Machine;
import com.factoryos.modules.tenant.domain.Plant;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "vibration_burst_samples")
public class VibrationBurstSample {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id", nullable = false)
    private Machine machine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plant_id")
    private Plant plant;

    @Column(nullable = false, length = 32)
    private String axis = "RADIAL_X";

    @Column(name = "sample_rate_hz", nullable = false)
    private double sampleRateHz = 2048.0;

    @Column(name = "sample_count", nullable = false)
    private int sampleCount = 1024;

    @Column(name = "running_speed_rpm")
    private Double runningSpeedRpm;

    @Column(name = "rms_velocity_mm_s", nullable = false)
    private double rmsVelocityMmS = 0.0;

    @Column(name = "peak_acceleration_g", nullable = false)
    private double peakAccelerationG = 0.0;

    @Column(name = "crest_factor", nullable = false)
    private double crestFactor = 0.0;

    @Column(nullable = false)
    private double kurtosis = 3.0;

    @Column(name = "bearing_temperature_c")
    private Double bearingTemperatureC;

    @Column(name = "raw_samples", columnDefinition = "TEXT")
    private String rawSamplesJson;

    @OneToMany(mappedBy = "burst", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VibrationSpectralPeak> spectralPeaks = new ArrayList<>();

    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt = Instant.now();

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
    @Column(nullable = false)
    private Long version = 0L;

    public VibrationBurstSample() {
    }

    public void addSpectralPeak(VibrationSpectralPeak peak) {
        spectralPeaks.add(peak);
        peak.setBurst(this);
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

    public String getAxis() {
        return axis;
    }

    public void setAxis(String axis) {
        this.axis = axis;
    }

    public double getSampleRateHz() {
        return sampleRateHz;
    }

    public void setSampleRateHz(double sampleRateHz) {
        this.sampleRateHz = sampleRateHz;
    }

    public int getSampleCount() {
        return sampleCount;
    }

    public void setSampleCount(int sampleCount) {
        this.sampleCount = sampleCount;
    }

    public Double getRunningSpeedRpm() {
        return runningSpeedRpm;
    }

    public void setRunningSpeedRpm(Double runningSpeedRpm) {
        this.runningSpeedRpm = runningSpeedRpm;
    }

    public double getRmsVelocityMmS() {
        return rmsVelocityMmS;
    }

    public void setRmsVelocityMmS(double rmsVelocityMmS) {
        this.rmsVelocityMmS = rmsVelocityMmS;
    }

    public double getPeakAccelerationG() {
        return peakAccelerationG;
    }

    public void setPeakAccelerationG(double peakAccelerationG) {
        this.peakAccelerationG = peakAccelerationG;
    }

    public double getCrestFactor() {
        return crestFactor;
    }

    public void setCrestFactor(double crestFactor) {
        this.crestFactor = crestFactor;
    }

    public double getKurtosis() {
        return kurtosis;
    }

    public void setKurtosis(double kurtosis) {
        this.kurtosis = kurtosis;
    }

    public Double getBearingTemperatureC() {
        return bearingTemperatureC;
    }

    public void setBearingTemperatureC(Double bearingTemperatureC) {
        this.bearingTemperatureC = bearingTemperatureC;
    }

    public String getRawSamplesJson() {
        return rawSamplesJson;
    }

    public void setRawSamplesJson(String rawSamplesJson) {
        this.rawSamplesJson = rawSamplesJson;
    }

    public List<VibrationSpectralPeak> getSpectralPeaks() {
        return spectralPeaks;
    }

    public void setSpectralPeaks(List<VibrationSpectralPeak> spectralPeaks) {
        this.spectralPeaks = spectralPeaks;
    }

    public Instant getCapturedAt() {
        return capturedAt;
    }

    public void setCapturedAt(Instant capturedAt) {
        this.capturedAt = capturedAt;
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

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
