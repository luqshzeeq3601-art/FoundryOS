package com.factoryos.modules.vibration.application;

import com.factoryos.modules.vibration.domain.FaultHarmonicType;
import com.factoryos.modules.vibration.dto.FftSpectrumDto;
import com.factoryos.modules.vibration.dto.SpectralPeakDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class FftSpectralAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(FftSpectralAnalysisService.class);

    private static final double GRAVITY_ACCEL = 9.80665; // m/s^2 per g
    private static final double TWO_PI = 2.0 * Math.PI;

    /**
     * Executes windowed Fast Fourier Transform (FFT) on raw time-domain vibration acceleration data (in g),
     * converts to velocity spectrum (mm/s), computes statistical metrics (RMS, Crest Factor, Kurtosis),
     * and identifies dominant harmonic fault peaks.
     */
    public FftSpectrumDto computeSpectrum(double[] samples, double sampleRateHz, Double runningSpeedRpm) {
        if (samples == null || samples.length < 4) {
            throw new IllegalArgumentException("Sample array must have at least 4 points for FFT analysis");
        }

        int originalN = samples.length;

        // 1. Statistical metrics on time domain
        double mean = 0.0;
        for (double s : samples) {
            mean += s;
        }
        mean /= originalN;

        double sumSq = 0.0;
        double peakAbs = 0.0;
        double sumPow4 = 0.0;
        for (double s : samples) {
            double dev = s - mean;
            sumSq += dev * dev;
            sumPow4 += dev * dev * dev * dev;
            double abs = Math.abs(s);
            if (abs > peakAbs) {
                peakAbs = abs;
            }
        }

        double variance = sumSq / originalN;
        double stdDev = Math.sqrt(variance);
        double accelRms = Math.sqrt(sumSq / originalN);
        double crestFactor = accelRms > 1e-6 ? (peakAbs / accelRms) : 0.0;
        double kurtosis = (variance > 1e-6) ? (sumPow4 / (originalN * variance * variance)) : 3.0;

        // 2. Pad to next power of 2 for Radix-2 Cooley-Tukey FFT
        int n = 1;
        while (n < originalN) {
            n <<= 1;
        }

        double[] real = new double[n];
        double[] imag = new double[n];

        // Apply Hann (Hanning) window to minimize spectral leakage
        // w[i] = 0.5 * (1 - cos(2*pi*i / (N - 1)))
        double windowSum = 0.0;
        for (int i = 0; i < originalN; i++) {
            double window = 0.5 * (1.0 - Math.cos(TWO_PI * i / (originalN - 1)));
            real[i] = (samples[i] - mean) * window;
            windowSum += window;
        }
        // Zero-pad remainder
        for (int i = originalN; i < n; i++) {
            real[i] = 0.0;
        }

        // Coherent gain factor for Hann window = windowSum / originalN (~0.5)
        double coherentGain = windowSum / originalN;

        // 3. Compute Cooley-Tukey Radix-2 FFT
        fftRadix2(real, imag);

        // 4. Frequency spectrum up to Nyquist limit (n / 2)
        int halfN = n / 2;
        double deltaF = sampleRateHz / n;

        double[] frequencies = new double[halfN];
        double[] velocityAmplitudes = new double[halfN];

        double runningFreqHz = (runningSpeedRpm != null && runningSpeedRpm > 0) ? (runningSpeedRpm / 60.0) : 50.0;

        // Convert acceleration spectrum to velocity in mm/s:
        // V(f) = (A(f) * 9.80665 * 1000) / (2 * pi * f) mm/s
        double sumVelSq = 0.0;
        for (int k = 0; k < halfN; k++) {
            double freq = k * deltaF;
            frequencies[k] = Math.round(freq * 100.0) / 100.0;

            // Magnitude in g peak
            double magG = (2.0 / (originalN * coherentGain)) * Math.sqrt(real[k] * real[k] + imag[k] * imag[k]);

            // Frequency domain integration to velocity (mm/s)
            if (freq >= 2.0) {
                double velMmS = (magG * GRAVITY_ACCEL * 1000.0) / (TWO_PI * freq);
                velocityAmplitudes[k] = Math.round(velMmS * 1000.0) / 1000.0;
                sumVelSq += velMmS * velMmS;
            } else {
                velocityAmplitudes[k] = 0.0;
            }
        }

        // Velocity RMS (ISO 10816 defines overall vibration velocity RMS over 10 Hz - 1000 Hz)
        double velocityRms = Math.sqrt(sumVelSq / 2.0); // RMS of sinusoidal components
        velocityRms = Math.round(velocityRms * 100.0) / 100.0;

        // 5. Detect dominant spectral peaks and classify harmonics
        List<SpectralPeakDto> peaks = detectPeaks(frequencies, velocityAmplitudes, runningFreqHz);

        FftSpectrumDto dto = new FftSpectrumDto();
        dto.setSampleRateHz(sampleRateHz);
        dto.setSampleCount(originalN);
        dto.setRunningSpeedRpm(runningSpeedRpm);
        dto.setFundamentalFrequencyHz(Math.round(runningFreqHz * 100.0) / 100.0);
        dto.setFrequencies(frequencies);
        dto.setAmplitudes(velocityAmplitudes);
        dto.setPeaks(peaks);
        dto.setRmsVelocityMmS(velocityRms);
        dto.setPeakAccelerationG(Math.round(peakAbs * 1000.0) / 1000.0);
        dto.setCrestFactor(Math.round(crestFactor * 100.0) / 100.0);
        dto.setKurtosis(Math.round(kurtosis * 100.0) / 100.0);

        return dto;
    }

    /**
     * Detects prominent local maxima in the velocity spectrum and maps them to known kinematic fault orders.
     */
    public List<SpectralPeakDto> detectPeaks(double[] frequencies, double[] amplitudes, double runningFreqHz) {
        List<SpectralPeakDto> detected = new ArrayList<>();
        if (frequencies.length < 3) return detected;

        // Compute average amplitude floor
        double sum = 0.0;
        for (double a : amplitudes) sum += a;
        double avgFloor = sum / amplitudes.length;
        double threshold = Math.max(0.05, avgFloor * 2.5);

        for (int i = 1; i < frequencies.length - 1; i++) {
            double prev = amplitudes[i - 1];
            double curr = amplitudes[i];
            double next = amplitudes[i + 1];

            if (curr > prev && curr > next && curr >= threshold) {
                double freq = frequencies[i];
                Double order = (runningFreqHz > 0) ? (Math.round((freq / runningFreqHz) * 100.0) / 100.0) : null;
                FaultHarmonicType faultType = classifyPeak(order, freq, runningFreqHz);
                double confidence = calculateConfidence(order, faultType);

                detected.add(new SpectralPeakDto(freq, curr, order, faultType, confidence));
            }
        }

        // Sort descending by amplitude and keep top 10
        detected.sort((a, b) -> Double.compare(b.getAmplitudeMmS(), a.getAmplitudeMmS()));
        if (detected.size() > 10) {
            return detected.subList(0, 10);
        }
        return detected;
    }

    private FaultHarmonicType classifyPeak(Double order, double freq, double runningFreqHz) {
        if (order == null) return FaultHarmonicType.NORMAL;

        // 1X Rotor Unbalance (0.95X - 1.05X)
        if (Math.abs(order - 1.0) <= 0.06) {
            return FaultHarmonicType.UNBALANCE_1X;
        }

        // 2X Shaft / Coupling Misalignment (1.92X - 2.08X)
        if (Math.abs(order - 2.0) <= 0.08) {
            return FaultHarmonicType.MISALIGNMENT_2X;
        }

        // 3X Mechanical Looseness (2.90X - 3.10X)
        if (Math.abs(order - 3.0) <= 0.10) {
            return FaultHarmonicType.LOOSENESS_3X;
        }

        // Typical BPFO (Ball Pass Frequency Outer Race) ~ 3.5X - 4.2X
        if (order >= 3.4 && order <= 4.3 && Math.abs(order - 4.0) > 0.12) {
            return FaultHarmonicType.BPFO_BEARING_OUTER;
        }

        // Typical BPFI (Ball Pass Frequency Inner Race) ~ 5.2X - 6.2X
        if (order >= 5.1 && order <= 6.3 && Math.abs(order - 5.0) > 0.12 && Math.abs(order - 6.0) > 0.12) {
            return FaultHarmonicType.BPFI_BEARING_INNER;
        }

        // High frequency friction / gear mesh
        if (freq > 800.0) {
            return FaultHarmonicType.HIGH_FREQUENCY_NOISE;
        }

        return FaultHarmonicType.NORMAL;
    }

    private double calculateConfidence(Double order, FaultHarmonicType type) {
        if (type == FaultHarmonicType.NORMAL) return 0.60;
        if (order == null) return 0.50;

        if (type == FaultHarmonicType.UNBALANCE_1X) {
            return Math.max(0.70, 1.0 - Math.abs(order - 1.0) * 4.0);
        }
        if (type == FaultHarmonicType.MISALIGNMENT_2X) {
            return Math.max(0.70, 1.0 - Math.abs(order - 2.0) * 3.5);
        }
        if (type == FaultHarmonicType.LOOSENESS_3X) {
            return Math.max(0.65, 1.0 - Math.abs(order - 3.0) * 3.0);
        }
        return 0.85;
    }

    /**
     * In-place Cooley-Tukey Radix-2 decimation-in-time FFT.
     * Length n must be an exact power of 2.
     */
    private void fftRadix2(double[] real, double[] imag) {
        int n = real.length;

        // Bit-reversal permutation
        int j = 0;
        for (int i = 0; i < n - 1; i++) {
            if (i < j) {
                double tempR = real[i];
                real[i] = real[j];
                real[j] = tempR;

                double tempI = imag[i];
                imag[i] = imag[j];
                imag[j] = tempI;
            }
            int k = n / 2;
            while (k <= j) {
                j -= k;
                k /= 2;
            }
            j += k;
        }

        // Cooley-Tukey butterfly computations
        for (int len = 2; len <= n; len <<= 1) {
            double angle = -TWO_PI / len;
            double wlenR = Math.cos(angle);
            double wlenI = Math.sin(angle);

            for (int i = 0; i < n; i += len) {
                double wR = 1.0;
                double wI = 0.0;

                for (int m = 0; m < len / 2; m++) {
                    int uIdx = i + m;
                    int vIdx = i + m + len / 2;

                    double uR = real[uIdx];
                    double uI = imag[uIdx];

                    double vR = real[vIdx] * wR - imag[vIdx] * wI;
                    double vI = real[vIdx] * wI + imag[vIdx] * wR;

                    real[uIdx] = uR + vR;
                    imag[uIdx] = uI + vI;

                    real[vIdx] = uR - vR;
                    imag[vIdx] = uI - vI;

                    double nextWR = wR * wlenR - wI * wlenI;
                    wI = wR * wlenI + wI * wlenR;
                    wR = nextWR;
                }
            }
        }
    }
}
