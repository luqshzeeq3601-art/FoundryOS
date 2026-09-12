package com.factoryos.modules.vibration.application;

import com.factoryos.modules.vibration.domain.FaultHarmonicType;
import com.factoryos.modules.vibration.dto.FftSpectrumDto;
import com.factoryos.modules.vibration.dto.SpectralPeakDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FftSpectralAnalysisServiceTest {

    private FftSpectralAnalysisService fftService;

    @BeforeEach
    void setUp() {
        fftService = new FftSpectralAnalysisService();
    }

    @Test
    void computeSpectrum_Pure50HzSineWave_IdentifiesFundamentalPeak() {
        double sampleRate = 2048.0;
        int n = 1024;
        double targetFreq = 50.0; // 50 Hz
        double amplitudeG = 1.0; // 1g peak

        double[] samples = new double[n];
        for (int i = 0; i < n; i++) {
            double t = (double) i / sampleRate;
            samples[i] = amplitudeG * Math.sin(2.0 * Math.PI * targetFreq * t);
        }

        // Running speed = 3000 RPM -> fundamental frequency = 50.0 Hz
        FftSpectrumDto spectrum = fftService.computeSpectrum(samples, sampleRate, 3000.0);

        assertNotNull(spectrum);
        assertEquals(sampleRate, spectrum.getSampleRateHz());
        assertEquals(n, spectrum.getSampleCount());
        assertEquals(50.0, spectrum.getFundamentalFrequencyHz());
        assertTrue(spectrum.getRmsVelocityMmS() > 0.0);

        // Peak detection verification
        List<SpectralPeakDto> peaks = spectrum.getPeaks();
        assertFalse(peaks.isEmpty(), "Spectrum must identify dominant 50 Hz peak");

        SpectralPeakDto dominant = peaks.get(0);
        assertEquals(50.0, dominant.getFrequencyHz(), 2.0, "Dominant peak should be at ~50 Hz");
        assertEquals(1.0, dominant.getOrderMultiple(), 0.05, "Order should be ~1.0X");
        assertEquals(FaultHarmonicType.UNBALANCE_1X, dominant.getFaultHarmonicType());
    }

    @Test
    void computeSpectrum_BearingOuterRaceBpfoFault_ClassifiesBpfoHarmonic() {
        double sampleRate = 2048.0;
        int n = 1024;
        double f0 = 50.0; // 1X = 50 Hz
        double bpfoFreq = 3.58 * f0; // 179.0 Hz (BPFO)

        double[] samples = new double[n];
        for (int i = 0; i < n; i++) {
            double t = (double) i / sampleRate;
            // Baseline 1X + prominent BPFO peak
            samples[i] = 0.20 * Math.sin(2.0 * Math.PI * f0 * t)
                       + 0.85 * Math.sin(2.0 * Math.PI * bpfoFreq * t);
        }

        FftSpectrumDto spectrum = fftService.computeSpectrum(samples, sampleRate, 3000.0);

        assertNotNull(spectrum);
        List<SpectralPeakDto> peaks = spectrum.getPeaks();
        assertFalse(peaks.isEmpty());

        boolean foundBpfo = peaks.stream()
                .anyMatch(p -> p.getFaultHarmonicType() == FaultHarmonicType.BPFO_BEARING_OUTER);
        assertTrue(foundBpfo, "Spectrum should detect BPFO_BEARING_OUTER harmonic peak");
    }

    @Test
    void computeSpectrum_Misalignment2X_Classifies2XHarmonic() {
        double sampleRate = 2048.0;
        int n = 1024;
        double f0 = 50.0; // 1X = 50 Hz
        double misalignmentFreq = 2.0 * f0; // 100.0 Hz (2X)

        double[] samples = new double[n];
        for (int i = 0; i < n; i++) {
            double t = (double) i / sampleRate;
            samples[i] = 0.30 * Math.sin(2.0 * Math.PI * f0 * t)
                       + 0.90 * Math.sin(2.0 * Math.PI * misalignmentFreq * t);
        }

        FftSpectrumDto spectrum = fftService.computeSpectrum(samples, sampleRate, 3000.0);

        assertNotNull(spectrum);
        List<SpectralPeakDto> peaks = spectrum.getPeaks();
        assertFalse(peaks.isEmpty());

        boolean foundMisalignment = peaks.stream()
                .anyMatch(p -> p.getFaultHarmonicType() == FaultHarmonicType.MISALIGNMENT_2X);
        assertTrue(foundMisalignment, "Spectrum should detect MISALIGNMENT_2X peak at 100 Hz");
    }

    @Test
    void computeSpectrum_StatisticalMetrics_CalculatesCorrectCrestFactorAndKurtosis() {
        double sampleRate = 1000.0;
        int n = 1000;
        double[] samples = new double[n];

        // Pure sine wave has theoretical crest factor = sqrt(2) ~ 1.414, kurtosis ~ 1.5
        for (int i = 0; i < n; i++) {
            samples[i] = 2.0 * Math.sin(2.0 * Math.PI * 10.0 * (i / sampleRate));
        }

        FftSpectrumDto spectrum = fftService.computeSpectrum(samples, sampleRate, 600.0);

        assertEquals(1.41, spectrum.getCrestFactor(), 0.1);
        assertTrue(spectrum.getKurtosis() > 1.0 && spectrum.getKurtosis() < 2.0);
    }
}
