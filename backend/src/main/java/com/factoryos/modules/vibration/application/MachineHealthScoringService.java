package com.factoryos.modules.vibration.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.factoryos.common.exception.AppException;
import com.factoryos.modules.audit.application.AuditRecordingService;
import com.factoryos.modules.auth.domain.User;
import com.factoryos.modules.machine.domain.Machine;
import com.factoryos.modules.machine.repository.MachineRepository;
import com.factoryos.modules.vibration.domain.*;
import com.factoryos.modules.vibration.dto.*;
import com.factoryos.modules.vibration.repository.MachineHealthAssessmentRepository;
import com.factoryos.modules.vibration.repository.VibrationBurstSampleRepository;
import com.factoryos.modules.vibration.repository.VibrationSpectralPeakRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class MachineHealthScoringService {

    private static final Logger log = LoggerFactory.getLogger(MachineHealthScoringService.class);

    private final FftSpectralAnalysisService fftService;
    private final Iso10816StandardsEngine isoEngine;
    private final VibrationBurstSampleRepository burstRepository;
    private final VibrationSpectralPeakRepository peakRepository;
    private final MachineHealthAssessmentRepository assessmentRepository;
    private final MachineRepository machineRepository;
    private final AuditRecordingService auditRecordingService;
    private final PrescriptiveMaintenanceService prescriptiveMaintenanceService;
    private final ObjectMapper objectMapper;

    public MachineHealthScoringService(
            FftSpectralAnalysisService fftService,
            Iso10816StandardsEngine isoEngine,
            VibrationBurstSampleRepository burstRepository,
            VibrationSpectralPeakRepository peakRepository,
            MachineHealthAssessmentRepository assessmentRepository,
            MachineRepository machineRepository,
            AuditRecordingService auditRecordingService,
            PrescriptiveMaintenanceService prescriptiveMaintenanceService,
            ObjectMapper objectMapper
    ) {
        this.fftService = fftService;
        this.isoEngine = isoEngine;
        this.burstRepository = burstRepository;
        this.peakRepository = peakRepository;
        this.assessmentRepository = assessmentRepository;
        this.machineRepository = machineRepository;
        this.auditRecordingService = auditRecordingService;
        this.prescriptiveMaintenanceService = prescriptiveMaintenanceService;
        this.objectMapper = objectMapper;
    }

    /**
     * Ingests a raw vibration burst, executes windowed FFT spectral analysis,
     * evaluates ISO 10816-3 severity, calculates composite machine health score,
     * and persists the assessment.
     */
    @Transactional
    public MachineHealthAssessmentDto processAndAssessBurst(VibrationBurstIngestDto ingestDto, User actor) {
        Machine machine = machineRepository.findByIdAndIsDeletedFalse(ingestDto.getMachineId())
                .orElseThrow(() -> AppException.notFound("Machine not found with ID: " + ingestDto.getMachineId()));

        List<Double> sampleList = ingestDto.getSamples();
        if (sampleList == null || sampleList.size() < 4) {
            throw AppException.badRequest("At least 4 vibration acceleration samples required");
        }

        double[] rawSamples = new double[sampleList.size()];
        for (int i = 0; i < sampleList.size(); i++) {
            rawSamples[i] = sampleList.get(i);
        }

        // 1. Perform FFT spectral analysis
        FftSpectrumDto spectrum = fftService.computeSpectrum(
                rawSamples,
                ingestDto.getSampleRateHz(),
                ingestDto.getRunningSpeedRpm()
        );
        spectrum.setMachineId(machine.getId());
        spectrum.setMachineName(machine.getName());
        spectrum.setAxis(ingestDto.getAxis());
        spectrum.setBearingTemperatureC(ingestDto.getBearingTemperatureC());

        // 2. Evaluate ISO 10816 severity
        MachineVibrationClass vibrationClass = MachineVibrationClass.CLASS_II_MEDIUM;
        IsoSeverityResultDto isoResult = isoEngine.evaluateSeverity(spectrum.getRmsVelocityMmS(), vibrationClass);

        // 3. Composite Health Score Calculation
        HealthScoreResult scoreResult = calculateHealthScore(
                spectrum,
                isoResult,
                ingestDto.getBearingTemperatureC()
        );

        // 4. Save VibrationBurstSample entity
        VibrationBurstSample burst = new VibrationBurstSample();
        burst.setMachine(machine);
        burst.setPlant(machine.getPlant());
        burst.setAxis(ingestDto.getAxis() != null ? ingestDto.getAxis() : "RADIAL_X");
        burst.setSampleRateHz(ingestDto.getSampleRateHz());
        burst.setSampleCount(rawSamples.length);
        burst.setRunningSpeedRpm(ingestDto.getRunningSpeedRpm());
        burst.setRmsVelocityMmS(spectrum.getRmsVelocityMmS());
        burst.setPeakAccelerationG(spectrum.getPeakAccelerationG());
        burst.setCrestFactor(spectrum.getCrestFactor());
        burst.setKurtosis(spectrum.getKurtosis());
        burst.setBearingTemperatureC(ingestDto.getBearingTemperatureC());
        burst.setCapturedAt(Instant.now());

        try {
            // Save downsampled points in rawSamplesJson if large
            int step = Math.max(1, rawSamples.length / 256);
            List<Double> condensed = new ArrayList<>();
            for (int i = 0; i < rawSamples.length; i += step) {
                condensed.add(Math.round(rawSamples[i] * 1000.0) / 1000.0);
            }
            burst.setRawSamplesJson(objectMapper.writeValueAsString(condensed));
        } catch (Exception e) {
            log.warn("Failed to serialize raw samples JSON", e);
        }

        VibrationBurstSample savedBurst = burstRepository.save(burst);

        // 5. Save Spectral Peaks
        for (SpectralPeakDto peakDto : spectrum.getPeaks()) {
            VibrationSpectralPeak peak = new VibrationSpectralPeak(
                    peakDto.getFrequencyHz(),
                    peakDto.getAmplitudeMmS(),
                    peakDto.getOrderMultiple(),
                    peakDto.getFaultHarmonicType(),
                    peakDto.getConfidence()
            );
            savedBurst.addSpectralPeak(peak);
            peakRepository.save(peak);
        }

        // 6. Save MachineHealthAssessment entity
        MachineHealthAssessment assessment = new MachineHealthAssessment();
        assessment.setMachine(machine);
        assessment.setPlant(machine.getPlant());
        assessment.setLatestBurst(savedBurst);
        assessment.setHealthScore(scoreResult.score);
        assessment.setHealthStatus(scoreResult.status);
        assessment.setIsoSeverityZone(isoResult.getZone());
        assessment.setVibrationClass(vibrationClass);
        assessment.setRmsVelocityMmS(spectrum.getRmsVelocityMmS());
        assessment.setSpindleTemperatureC(ingestDto.getBearingTemperatureC());
        assessment.setDominantFaultType(scoreResult.dominantFault.name());
        assessment.setDiagnosisSummary(scoreResult.diagnosis);
        assessment.setRecommendedAction(scoreResult.recommendation);
        assessment.setAssessedAt(Instant.now());

        MachineHealthAssessment savedAssessment = assessmentRepository.save(assessment);

        // 7. Audit if in Warning or Critical
        if (scoreResult.status == MachineHealthStatus.WARNING || scoreResult.status == MachineHealthStatus.CRITICAL) {
            auditRecordingService.record(
                    actor != null ? actor.getId() : null,
                    "MACHINE_HEALTH_ALERT",
                    "Machine",
                    machine.getId(),
                    null,
                    Map.of(
                            "machineName", machine.getName(),
                            "healthScore", scoreResult.score,
                            "healthStatus", scoreResult.status.name(),
                            "isoZone", isoResult.getZone().name(),
                            "dominantFault", scoreResult.dominantFault.name(),
                            "rmsVelocity", spectrum.getRmsVelocityMmS()
                    )
            );
        }

        // 8. Trigger Prescriptive Automated Maintenance Work Order Generation / Deduplication
        List<VibrationSpectralPeak> savedPeaks = peakRepository.findByBurstIdOrderByAmplitudeMmSDesc(savedBurst.getId());
        Optional<com.factoryos.modules.maintenance.dto.WorkOrderDto> prescriptiveOrder = 
                prescriptiveMaintenanceService.evaluateAndCreatePrescriptiveOrder(savedAssessment, savedBurst, savedPeaks, actor);

        MachineHealthAssessmentDto assessmentDto = toAssessmentDto(savedAssessment, spectrum.getPeaks());
        prescriptiveOrder.ifPresent(assessmentDto::setActiveWorkOrder);
        return assessmentDto;
    }

    /**
     * Synthesizes realistic time-domain vibration burst signals for rehearsal and testing.
     */
    @Transactional
    public MachineHealthAssessmentDto simulateBurst(SimulateBurstRequestDto request, User actor) {
        Machine machine = machineRepository.findByIdAndIsDeletedFalse(request.getMachineId())
                .orElseThrow(() -> AppException.notFound("Machine not found with ID: " + request.getMachineId()));

        double rpm = request.getRunningSpeedRpm() > 0 ? request.getRunningSpeedRpm() : 3000.0;
        double f0 = rpm / 60.0; // fundamental running frequency Hz (e.g. 50.0 Hz)
        double fs = request.getSampleRateHz() > 0 ? request.getSampleRateHz() : 2048.0;
        int n = request.getSampleCount() > 0 ? request.getSampleCount() : 1024;
        double noise = request.getNoiseLevel() > 0 ? request.getNoiseLevel() : 0.05;

        Random random = new Random(42);
        List<Double> samples = new ArrayList<>(n);

        FaultHarmonicType fault = request.getFaultType() != null ? request.getFaultType() : FaultHarmonicType.NORMAL;
        double defaultTemp = 42.0;

        for (int i = 0; i < n; i++) {
            double t = (double) i / fs;
            double val = 0.0;

            // Gaussian noise baseline
            val += random.nextGaussian() * noise;

            switch (fault) {
                case NORMAL:
                    // Pure baseline running speed (nominal 1X ~0.15g)
                    val += 0.15 * Math.sin(2.0 * Math.PI * f0 * t);
                    defaultTemp = 42.0 + random.nextDouble() * 2.0;
                    break;

                case UNBALANCE_1X:
                    // Heavy 1X unbalance (~0.85g at 1X)
                    val += 0.85 * Math.sin(2.0 * Math.PI * f0 * t);
                    val += 0.10 * Math.sin(2.0 * Math.PI * 2.0 * f0 * t);
                    defaultTemp = 58.0;
                    break;

                case MISALIGNMENT_2X:
                    // Strong 2X shaft misalignment (~0.55g at 1X, ~0.95g at 2X)
                    val += 0.55 * Math.sin(2.0 * Math.PI * f0 * t);
                    val += 0.95 * Math.sin(2.0 * Math.PI * 2.0 * f0 * t + Math.PI / 4.0);
                    val += 0.25 * Math.sin(2.0 * Math.PI * 3.0 * f0 * t);
                    defaultTemp = 68.5;
                    break;

                case LOOSENESS_3X:
                    // Elevated 3X and half-order harmonics
                    val += 0.40 * Math.sin(2.0 * Math.PI * f0 * t);
                    val += 0.35 * Math.sin(2.0 * Math.PI * 2.0 * f0 * t);
                    val += 0.80 * Math.sin(2.0 * Math.PI * 3.0 * f0 * t);
                    val += 0.20 * Math.sin(2.0 * Math.PI * 0.5 * f0 * t);
                    defaultTemp = 62.0;
                    break;

                case BPFO_BEARING_OUTER:
                    // 1X baseline + prominent BPFO (~3.58 * f0) with repetitive impact spikes
                    double bpfo = 3.58 * f0;
                    val += 0.15 * Math.sin(2.0 * Math.PI * f0 * t);
                    val += 1.40 * Math.sin(2.0 * Math.PI * bpfo * t);
                    val += 0.65 * Math.sin(2.0 * Math.PI * 2.0 * bpfo * t);
                    // Impulse impacts every 1/bpfo seconds
                    if ((i % (int) Math.round(fs / bpfo)) == 0) {
                        val += 2.5 * (random.nextBoolean() ? 1.0 : -1.0);
                    }
                    defaultTemp = 78.0;
                    break;

                case BPFI_BEARING_INNER:
                    // 1X baseline + BPFI (~5.42 * f0) modulated at running speed
                    double bpfi = 5.42 * f0;
                    val += 0.30 * Math.sin(2.0 * Math.PI * f0 * t);
                    double carrier = Math.sin(2.0 * Math.PI * bpfi * t);
                    double mod = (1.0 + 0.5 * Math.sin(2.0 * Math.PI * f0 * t));
                    val += 1.10 * carrier * mod;
                    defaultTemp = 82.5;
                    break;

                default:
                    val += 0.20 * Math.sin(2.0 * Math.PI * f0 * t);
                    break;
            }

            samples.add(val);
        }

        VibrationBurstIngestDto ingest = new VibrationBurstIngestDto();
        ingest.setMachineId(machine.getId());
        ingest.setAxis("RADIAL_X");
        ingest.setSampleRateHz(fs);
        ingest.setRunningSpeedRpm(rpm);
        ingest.setBearingTemperatureC(request.getBearingTemperatureC() != null ? request.getBearingTemperatureC() : defaultTemp);
        ingest.setSamples(samples);

        return processAndAssessBurst(ingest, actor);
    }

    public MachineHealthAssessmentDto getLatestAssessment(UUID machineId) {
        MachineHealthAssessment assessment = assessmentRepository.findFirstByMachineIdAndIsDeletedFalseOrderByAssessedAtDesc(machineId)
                .orElseThrow(() -> AppException.notFound("No health assessment found for machine: " + machineId));

        List<SpectralPeakDto> peaks = List.of();
        if (assessment.getLatestBurst() != null) {
            peaks = peakRepository.findByBurstIdOrderByAmplitudeMmSDesc(assessment.getLatestBurst().getId()).stream()
                    .map(p -> new SpectralPeakDto(p.getFrequencyHz(), p.getAmplitudeMmS(), p.getOrderMultiple(), p.getFaultHarmonicType(), p.getConfidence()))
                    .collect(Collectors.toList());
        }

        return toAssessmentDto(assessment, peaks);
    }

    public List<MachineHealthAssessmentDto> getAssessmentHistory(UUID machineId) {
        return assessmentRepository.findTop20ByMachineIdAndIsDeletedFalseOrderByAssessedAtDesc(machineId).stream()
                .map(a -> toAssessmentDto(a, List.of()))
                .collect(Collectors.toList());
    }

    public FleetHealthSummaryDto getFleetHealthSummary(UUID plantId) {
        List<MachineHealthAssessment> assessments = plantId != null
                ? assessmentRepository.findLatestAssessmentsByPlant(plantId)
                : assessmentRepository.findLatestAssessmentsForAllMachines();

        FleetHealthSummaryDto summary = new FleetHealthSummaryDto();
        summary.setTotalMachinesAssessed(assessments.size());

        if (assessments.isEmpty()) {
            summary.setAverageFleetHealthScore(100.0);
            return summary;
        }

        double totalScore = 0;
        int zoneA = 0, zoneB = 0, zoneC = 0, zoneD = 0;
        int crit = 0, warn = 0, fair = 0, healthy = 0;

        List<MachineHealthAssessmentDto> dtos = new ArrayList<>();
        for (MachineHealthAssessment a : assessments) {
            totalScore += a.getHealthScore();
            if (a.getIsoSeverityZone() == IsoSeverityZone.ZONE_A) zoneA++;
            else if (a.getIsoSeverityZone() == IsoSeverityZone.ZONE_B) zoneB++;
            else if (a.getIsoSeverityZone() == IsoSeverityZone.ZONE_C) zoneC++;
            else if (a.getIsoSeverityZone() == IsoSeverityZone.ZONE_D) zoneD++;

            if (a.getHealthStatus() == MachineHealthStatus.CRITICAL) crit++;
            else if (a.getHealthStatus() == MachineHealthStatus.WARNING) warn++;
            else if (a.getHealthStatus() == MachineHealthStatus.FAIR_DEGRADED) fair++;
            else healthy++;

            dtos.add(toAssessmentDto(a, List.of()));
        }

        summary.setAverageFleetHealthScore(Math.round((totalScore / assessments.size()) * 10.0) / 10.0);
        summary.setZoneACount(zoneA);
        summary.setZoneBCount(zoneB);
        summary.setZoneCCount(zoneC);
        summary.setZoneDCount(zoneD);
        summary.setCriticalCount(crit);
        summary.setWarningCount(warn);
        summary.setFairCount(fair);
        summary.setHealthyCount(healthy);
        summary.setAssessments(dtos);

        return summary;
    }

    public FftSpectrumDto getLatestSpectrum(UUID machineId) {
        VibrationBurstSample burst = burstRepository.findFirstByMachineIdAndIsDeletedFalseOrderByCapturedAtDesc(machineId)
                .orElseThrow(() -> AppException.notFound("No vibration burst found for machine: " + machineId));

        List<VibrationSpectralPeak> peaks = peakRepository.findByBurstIdOrderByAmplitudeMmSDesc(burst.getId());

        FftSpectrumDto dto = new FftSpectrumDto();
        dto.setMachineId(burst.getMachine().getId());
        dto.setMachineName(burst.getMachine().getName());
        dto.setAxis(burst.getAxis());
        dto.setSampleRateHz(burst.getSampleRateHz());
        dto.setSampleCount(burst.getSampleCount());
        dto.setRunningSpeedRpm(burst.getRunningSpeedRpm());
        dto.setFundamentalFrequencyHz(burst.getRunningSpeedRpm() != null ? (burst.getRunningSpeedRpm() / 60.0) : 50.0);
        dto.setRmsVelocityMmS(burst.getRmsVelocityMmS());
        dto.setPeakAccelerationG(burst.getPeakAccelerationG());
        dto.setCrestFactor(burst.getCrestFactor());
        dto.setKurtosis(burst.getKurtosis());
        dto.setBearingTemperatureC(burst.getBearingTemperatureC());
        dto.setCapturedAt(burst.getCapturedAt());

        dto.setPeaks(peaks.stream()
                .map(p -> new SpectralPeakDto(p.getFrequencyHz(), p.getAmplitudeMmS(), p.getOrderMultiple(), p.getFaultHarmonicType(), p.getConfidence()))
                .collect(Collectors.toList()));

        // Synthesize frequency axis up to 1000 Hz for visualization
        int bins = 128;
        double maxF = 1000.0;
        double[] freqs = new double[bins];
        double[] amps = new double[bins];
        double delta = maxF / bins;

        for (int i = 0; i < bins; i++) {
            freqs[i] = Math.round(i * delta * 10.0) / 10.0;
            amps[i] = 0.05 + (burst.getRmsVelocityMmS() * 0.04);
        }
        // Superimpose peaks onto curve
        for (VibrationSpectralPeak p : peaks) {
            int binIdx = (int) Math.round(p.getFrequencyHz() / delta);
            if (binIdx >= 0 && binIdx < bins) {
                amps[binIdx] = Math.max(amps[binIdx], p.getAmplitudeMmS());
                if (binIdx > 0) amps[binIdx - 1] = Math.max(amps[binIdx - 1], p.getAmplitudeMmS() * 0.6);
                if (binIdx < bins - 1) amps[binIdx + 1] = Math.max(amps[binIdx + 1], p.getAmplitudeMmS() * 0.6);
            }
        }
        dto.setFrequencies(freqs);
        dto.setAmplitudes(amps);

        return dto;
    }

    private HealthScoreResult calculateHealthScore(FftSpectrumDto spectrum, IsoSeverityResultDto iso, Double temperatureC) {
        int penalty = 0;

        // 1. ISO Vibration Severity Penalty (0 - 50 points)
        if (iso.getZone() == IsoSeverityZone.ZONE_A) {
            penalty += 0;
        } else if (iso.getZone() == IsoSeverityZone.ZONE_B) {
            penalty += 5;
        } else if (iso.getZone() == IsoSeverityZone.ZONE_C) {
            penalty += 32;
        } else {
            penalty += 55;
        }

        // 2. Temperature Penalty (0 - 30 points)
        if (temperatureC != null) {
            if (temperatureC > 85.0) {
                penalty += 30;
            } else if (temperatureC > 70.0) {
                penalty += 18;
            } else if (temperatureC > 58.0) {
                penalty += 8;
            }
        }

        // 3. Peak Harmonic Fault Penalty & Kurtosis (0 - 25 points)
        FaultHarmonicType dominantFault = FaultHarmonicType.NORMAL;
        double maxFaultAmp = 0.0;

        // Check if any bearing defect harmonics are present
        for (SpectralPeakDto p : spectrum.getPeaks()) {
            FaultHarmonicType fType = p.getFaultHarmonicType();
            if (fType == FaultHarmonicType.BPFO_BEARING_OUTER || fType == FaultHarmonicType.BPFI_BEARING_INNER || fType == FaultHarmonicType.BSF_BALL_SPIN) {
                dominantFault = fType;
                maxFaultAmp = p.getAmplitudeMmS();
                break;
            }
        }

        // If no bearing defect, check other fault harmonics
        if (dominantFault == FaultHarmonicType.NORMAL) {
            for (SpectralPeakDto p : spectrum.getPeaks()) {
                FaultHarmonicType fType = p.getFaultHarmonicType();
                if (fType == FaultHarmonicType.MISALIGNMENT_2X || fType == FaultHarmonicType.LOOSENESS_3X) {
                    if (p.getAmplitudeMmS() > maxFaultAmp) {
                        dominantFault = fType;
                        maxFaultAmp = p.getAmplitudeMmS();
                    }
                } else if (fType == FaultHarmonicType.UNBALANCE_1X) {
                    // Only flag 1X unbalance if amplitude is actually elevated (> 1.8 mm/s or Zone B/C/D)
                    if (p.getAmplitudeMmS() >= 1.8 && p.getAmplitudeMmS() > maxFaultAmp) {
                        dominantFault = fType;
                        maxFaultAmp = p.getAmplitudeMmS();
                    }
                }
            }
        }

        if (dominantFault == FaultHarmonicType.BPFO_BEARING_OUTER || dominantFault == FaultHarmonicType.BPFI_BEARING_INNER) {
            penalty += 22;
        } else if (dominantFault == FaultHarmonicType.MISALIGNMENT_2X) {
            penalty += 16;
        } else if (dominantFault == FaultHarmonicType.UNBALANCE_1X) {
            penalty += 10;
        } else if (dominantFault == FaultHarmonicType.LOOSENESS_3X) {
            penalty += 12;
        }

        // Kurtosis impact penalty (> 4.5 indicates high-energy impulsive bearing impacts)
        if (spectrum.getKurtosis() > 5.0) {
            penalty += 10;
        } else if (spectrum.getKurtosis() > 4.0) {
            penalty += 5;
        }

        int score = Math.max(0, Math.min(100, 100 - penalty));
        MachineHealthStatus status = MachineHealthStatus.fromScore(score);

        // Generate narrative diagnosis & recommendations
        StringBuilder diag = new StringBuilder();
        StringBuilder rec = new StringBuilder();

        diag.append(String.format("Vibration velocity RMS is %.2f mm/s (%s, ISO 10816 %s). ",
                spectrum.getRmsVelocityMmS(), iso.getVibrationClass().getDescription(), iso.getZone().name()));

        if (temperatureC != null) {
            diag.append(String.format("Bearing temperature is %.1f°C. ", temperatureC));
        }

        if (dominantFault != FaultHarmonicType.NORMAL) {
            diag.append(String.format("Prominent fault signature detected: %s (amplitude %.2f mm/s). ",
                    dominantFault.getTitle(), maxFaultAmp));
        } else {
            diag.append("No abnormal bearing or alignment defect harmonics detected. ");
        }

        if (status == MachineHealthStatus.EXCELLENT || status == MachineHealthStatus.GOOD) {
            rec.append("Continue standard continuous monitoring. Machine kinematics nominal.");
        } else if (status == MachineHealthStatus.FAIR_DEGRADED) {
            rec.append("Schedule non-emergency mechanical inspection and bearing lubrication check during next shift transition.");
        } else if (status == MachineHealthStatus.WARNING) {
            rec.append("Immediate inspection required within 24-48 hours. Verify shaft laser alignment, bearing clearances, and lubrication condition.");
        } else {
            rec.append("CRITICAL: Severe risk of spindle bearing seizure or catastrophic failure. Initiate controlled machine trip and lock out.");
        }

        return new HealthScoreResult(score, status, dominantFault, diag.toString(), rec.toString());
    }

    private MachineHealthAssessmentDto toAssessmentDto(MachineHealthAssessment a, List<SpectralPeakDto> peaks) {
        MachineHealthAssessmentDto dto = new MachineHealthAssessmentDto();
        dto.setId(a.getId());
        dto.setMachineId(a.getMachine().getId());
        dto.setMachineName(a.getMachine().getName());
        dto.setSerialNumber(a.getMachine().getSerialNumber());
        dto.setLocation(a.getMachine().getLocation());
        if (a.getPlant() != null) {
            dto.setPlantId(a.getPlant().getId());
            dto.setPlantName(a.getPlant().getName());
        }
        dto.setHealthScore(a.getHealthScore());
        dto.setHealthStatus(a.getHealthStatus());
        dto.setHealthStatusDescription(a.getHealthStatus().getDescription());
        dto.setIsoSeverityZone(a.getIsoSeverityZone());
        dto.setIsoZoneTitle(a.getIsoSeverityZone().getTitle());
        dto.setVibrationClass(a.getVibrationClass());
        dto.setRmsVelocityMmS(a.getRmsVelocityMmS());
        dto.setSpindleTemperatureC(a.getSpindleTemperatureC());
        dto.setDominantFaultType(a.getDominantFaultType());
        dto.setDiagnosisSummary(a.getDiagnosisSummary());
        dto.setRecommendedAction(a.getRecommendedAction());
        dto.setDominantPeaks(peaks);
        dto.setAssessedAt(a.getAssessedAt());
        return dto;
    }

    private static class HealthScoreResult {
        final int score;
        final MachineHealthStatus status;
        final FaultHarmonicType dominantFault;
        final String diagnosis;
        final String recommendation;

        HealthScoreResult(int score, MachineHealthStatus status, FaultHarmonicType dominantFault, String diagnosis, String recommendation) {
            this.score = score;
            this.status = status;
            this.dominantFault = dominantFault;
            this.diagnosis = diagnosis;
            this.recommendation = recommendation;
        }
    }
}
