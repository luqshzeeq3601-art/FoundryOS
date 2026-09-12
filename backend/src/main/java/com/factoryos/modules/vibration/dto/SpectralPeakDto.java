package com.factoryos.modules.vibration.dto;

import com.factoryos.modules.vibration.domain.FaultHarmonicType;

public class SpectralPeakDto {

    private double frequencyHz;
    private double amplitudeMmS;
    private Double orderMultiple;
    private FaultHarmonicType faultHarmonicType;
    private double confidence;

    public SpectralPeakDto() {
    }

    public SpectralPeakDto(double frequencyHz, double amplitudeMmS, Double orderMultiple, FaultHarmonicType faultHarmonicType, double confidence) {
        this.frequencyHz = frequencyHz;
        this.amplitudeMmS = amplitudeMmS;
        this.orderMultiple = orderMultiple;
        this.faultHarmonicType = faultHarmonicType;
        this.confidence = confidence;
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
}
