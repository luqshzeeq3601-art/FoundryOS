package com.factoryos.modules.vibration.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.factoryos.modules.audit.application.AuditRecordingService;
import com.factoryos.modules.machine.domain.Machine;
import com.factoryos.modules.machine.repository.MachineRepository;
import com.factoryos.modules.vibration.domain.*;
import com.factoryos.modules.vibration.dto.MachineHealthAssessmentDto;
import com.factoryos.modules.vibration.dto.SimulateBurstRequestDto;
import com.factoryos.modules.vibration.dto.VibrationBurstIngestDto;
import com.factoryos.modules.vibration.repository.MachineHealthAssessmentRepository;
import com.factoryos.modules.vibration.repository.VibrationBurstSampleRepository;
import com.factoryos.modules.vibration.repository.VibrationSpectralPeakRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MachineHealthScoringServiceTest {

    @Mock
    private VibrationBurstSampleRepository burstRepository;

    @Mock
    private VibrationSpectralPeakRepository peakRepository;

    @Mock
    private MachineHealthAssessmentRepository assessmentRepository;

    @Mock
    private MachineRepository machineRepository;

    @Mock
    private AuditRecordingService auditRecordingService;

    @Mock
    private PrescriptiveMaintenanceService prescriptiveMaintenanceService;

    private FftSpectralAnalysisService fftService;
    private Iso10816StandardsEngine isoEngine;
    private MachineHealthScoringService scoringService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        fftService = new FftSpectralAnalysisService();
        isoEngine = new Iso10816StandardsEngine();
        objectMapper = new ObjectMapper();

        scoringService = new MachineHealthScoringService(
                fftService,
                isoEngine,
                burstRepository,
                peakRepository,
                assessmentRepository,
                machineRepository,
                auditRecordingService,
                prescriptiveMaintenanceService,
                objectMapper
        );
    }

    @Test
    void processAndAssessBurst_HealthySineSignal_YieldsExcellentHealthScore() {
        UUID machineId = UUID.randomUUID();
        Machine machine = new Machine();
        machine.setId(machineId);
        machine.setName("CNC Mill 01");

        when(machineRepository.findByIdAndIsDeletedFalse(machineId)).thenReturn(Optional.of(machine));
        when(burstRepository.save(any(VibrationBurstSample.class))).thenAnswer(i -> {
            VibrationBurstSample b = i.getArgument(0);
            b.setId(UUID.randomUUID());
            return b;
        });
        when(assessmentRepository.save(any(MachineHealthAssessment.class))).thenAnswer(i -> {
            MachineHealthAssessment a = i.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        // Generate clean low-amplitude 50 Hz burst (0.03g amplitude -> ~0.94 mm/s velocity, firmly Zone A)
        List<Double> samples = new ArrayList<>();
        for (int i = 0; i < 512; i++) {
            double t = (double) i / 1024.0;
            samples.add(0.03 * Math.sin(2.0 * Math.PI * 50.0 * t));
        }

        VibrationBurstIngestDto ingest = new VibrationBurstIngestDto();
        ingest.setMachineId(machineId);
        ingest.setSampleRateHz(1024.0);
        ingest.setRunningSpeedRpm(3000.0);
        ingest.setBearingTemperatureC(42.0); // nominal temp
        ingest.setSamples(samples);

        MachineHealthAssessmentDto assessment = scoringService.processAndAssessBurst(ingest, null);

        assertNotNull(assessment);
        assertTrue(assessment.getHealthScore() >= 90, "Clean vibration should have health score >= 90");
        assertEquals(MachineHealthStatus.EXCELLENT, assessment.getHealthStatus());
        assertEquals(IsoSeverityZone.ZONE_A, assessment.getIsoSeverityZone());
        assertTrue(assessment.getRmsVelocityMmS() < 2.0);
    }

    @Test
    void simulateBurst_BearingFaultBpfo_YieldsDegradedHealthScoreAndBpfoDiagnosis() {
        UUID machineId = UUID.randomUUID();
        Machine machine = new Machine();
        machine.setId(machineId);
        machine.setName("Heavy Stamping Press");

        when(machineRepository.findByIdAndIsDeletedFalse(machineId)).thenReturn(Optional.of(machine));
        when(burstRepository.save(any(VibrationBurstSample.class))).thenAnswer(i -> {
            VibrationBurstSample b = i.getArgument(0);
            b.setId(UUID.randomUUID());
            return b;
        });
        when(assessmentRepository.save(any(MachineHealthAssessment.class))).thenAnswer(i -> {
            MachineHealthAssessment a = i.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        SimulateBurstRequestDto simReq = new SimulateBurstRequestDto();
        simReq.setMachineId(machineId);
        simReq.setFaultType(FaultHarmonicType.BPFO_BEARING_OUTER);
        simReq.setRunningSpeedRpm(3000.0);
        simReq.setSampleRateHz(2048.0);
        simReq.setSampleCount(1024);
        simReq.setBearingTemperatureC(78.0); // high temp

        MachineHealthAssessmentDto assessment = scoringService.simulateBurst(simReq, null);

        assertNotNull(assessment);
        assertTrue(assessment.getHealthScore() < 75, "Bearing fault with elevated temp should degrade health score (< 75)");
        assertTrue(assessment.getDominantFaultType().contains("BPFO"));
        assertTrue(assessment.getDiagnosisSummary().contains("fault signature detected"));
        assertNotNull(assessment.getRecommendedAction());
    }

    @Test
    void simulateBurst_CriticalMisalignment_AuditsWarningOrCriticalEvent() {
        UUID machineId = UUID.randomUUID();
        Machine machine = new Machine();
        machine.setId(machineId);
        machine.setName("CNC Line 1 Spindle");

        when(machineRepository.findByIdAndIsDeletedFalse(machineId)).thenReturn(Optional.of(machine));
        when(burstRepository.save(any(VibrationBurstSample.class))).thenAnswer(i -> {
            VibrationBurstSample b = i.getArgument(0);
            b.setId(UUID.randomUUID());
            return b;
        });
        when(assessmentRepository.save(any(MachineHealthAssessment.class))).thenAnswer(i -> {
            MachineHealthAssessment a = i.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        SimulateBurstRequestDto simReq = new SimulateBurstRequestDto();
        simReq.setMachineId(machineId);
        simReq.setFaultType(FaultHarmonicType.MISALIGNMENT_2X);
        simReq.setRunningSpeedRpm(3000.0);
        simReq.setSampleRateHz(2048.0);
        simReq.setSampleCount(1024);
        simReq.setBearingTemperatureC(88.0); // Critical temp

        MachineHealthAssessmentDto assessment = scoringService.simulateBurst(simReq, null);

        assertNotNull(assessment);
        assertTrue(assessment.getHealthScore() < 60);
        assertTrue(assessment.getHealthStatus() == MachineHealthStatus.WARNING || assessment.getHealthStatus() == MachineHealthStatus.CRITICAL);
        verify(auditRecordingService).record(isNull(), eq("MACHINE_HEALTH_ALERT"), eq("Machine"), eq(machineId), isNull(), anyMap());
    }
}
