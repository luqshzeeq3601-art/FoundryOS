package com.factoryos.modules.telemetry.application;

import com.factoryos.common.exception.AppException;
import com.factoryos.modules.audit.application.AuditRecordingService;
import com.factoryos.modules.auth.domain.User;
import com.factoryos.modules.downtime.application.AutomatedDowntimeDetectionService;
import com.factoryos.modules.machine.domain.Machine;
import com.factoryos.modules.machine.domain.MachineStatus;
import com.factoryos.modules.machine.repository.MachineRepository;
import com.factoryos.modules.telemetry.domain.MachineTagMapping;
import com.factoryos.modules.telemetry.domain.MachineTelemetryPoint;
import com.factoryos.modules.telemetry.domain.ProtocolType;
import com.factoryos.modules.telemetry.domain.TelemetryQuality;
import com.factoryos.modules.telemetry.dto.*;
import com.factoryos.modules.telemetry.repository.MachineTagMappingRepository;
import com.factoryos.modules.telemetry.repository.MachineTelemetryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelemetryServiceTest {

    @Mock
    private MachineRepository machineRepository;

    @Mock
    private MachineTagMappingRepository tagMappingRepository;

    @Mock
    private MachineTelemetryRepository telemetryRepository;

    @Mock
    private AuditRecordingService auditRecordingService;

    @Mock
    private AutomatedDowntimeDetectionService automatedDowntimeDetectionService;

    @InjectMocks
    private TelemetryIngestionService telemetryService;

    private Machine testMachine;
    private User testUser;
    private UUID machineId;

    @BeforeEach
    void setUp() {
        machineId = UUID.randomUUID();
        testMachine = new Machine();
        testMachine.setId(machineId);
        testMachine.setName("CNC Mill #04");
        testMachine.setSerialNumber("CNC-2026-004");
        testMachine.setStatus(MachineStatus.RUNNING);

        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("controls@factoryos.local");
        testUser.setDisplayName("Test Controls Engineer");
    }

    @Test
    @DisplayName("Successfully create machine tag mapping with audit logging")
    void testCreateTagMappingSuccess() {
        CreateTagMappingRequest request = new CreateTagMappingRequest(
                "SPINDLE_SPEED",
                ProtocolType.OPC_UA,
                "ns=2;s=Device1.SpindleSpeed",
                "DOUBLE",
                "RPM",
                BigDecimal.valueOf(1.0)
        );

        when(machineRepository.findByIdAndIsDeletedFalse(machineId)).thenReturn(Optional.of(testMachine));
        when(tagMappingRepository.existsByMachineIdAndTagNameAndIsDeletedFalse(machineId, "SPINDLE_SPEED")).thenReturn(false);
        when(tagMappingRepository.save(any(MachineTagMapping.class))).thenAnswer(invocation -> {
            MachineTagMapping mapping = invocation.getArgument(0);
            mapping.setId(UUID.randomUUID());
            return mapping;
        });

        TagMappingDto dto = telemetryService.createTagMapping(machineId, request, testUser);

        assertThat(dto).isNotNull();
        assertThat(dto.getTagName()).isEqualTo("SPINDLE_SPEED");
        assertThat(dto.getProtocol()).isEqualTo(ProtocolType.OPC_UA);
        assertThat(dto.getMachineName()).isEqualTo("CNC Mill #04");

        verify(auditRecordingService, times(1)).record(
                eq(testUser.getId()),
                eq("TAG_MAPPING_CREATED"),
                eq("MachineTagMapping"),
                any(UUID.class),
                isNull(),
                any()
        );
    }

    @Test
    @DisplayName("Reject duplicate tag mapping creation on same machine")
    void testCreateTagMappingDuplicateConflict() {
        CreateTagMappingRequest request = new CreateTagMappingRequest(
                "SPINDLE_SPEED",
                ProtocolType.OPC_UA,
                "ns=2;s=Device1.SpindleSpeed",
                "DOUBLE",
                "RPM",
                BigDecimal.valueOf(1.0)
        );

        when(machineRepository.findByIdAndIsDeletedFalse(machineId)).thenReturn(Optional.of(testMachine));
        when(tagMappingRepository.existsByMachineIdAndTagNameAndIsDeletedFalse(machineId, "SPINDLE_SPEED")).thenReturn(true);

        assertThatThrownBy(() -> telemetryService.createTagMapping(machineId, request, testUser))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("already exists");

        verify(tagMappingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Batch ingestion applies tag scaling factor and triggers ISO vibration alert")
    void testIngestBatchWithScalingAndAlerts() {
        // Tag mapping with 0.1 scale factor (e.g. raw integer decibels or scaled current)
        MachineTagMapping vibMapping = new MachineTagMapping();
        vibMapping.setId(UUID.randomUUID());
        vibMapping.setMachine(testMachine);
        vibMapping.setTagName("VIBRATION_RMS");
        vibMapping.setScaleFactor(BigDecimal.valueOf(1.0));
        vibMapping.setUnitOfMeasure("mm/s");

        MachineTagMapping tempMapping = new MachineTagMapping();
        tempMapping.setId(UUID.randomUUID());
        tempMapping.setMachine(testMachine);
        tempMapping.setTagName("BEARING_TEMP");
        tempMapping.setScaleFactor(BigDecimal.valueOf(1.0));
        tempMapping.setUnitOfMeasure("°C");

        when(machineRepository.findByIdAndIsDeletedFalse(machineId)).thenReturn(Optional.of(testMachine));
        when(tagMappingRepository.findActiveMappingsByMachineId(machineId)).thenReturn(List.of(vibMapping, tempMapping));

        TelemetryBatchIngestRequest request = new TelemetryBatchIngestRequest(
                machineId,
                "GW-EDGE-01",
                List.of(
                        new TelemetryPointDto("SPINDLE_SPEED", 8500.0, "RPM", TelemetryQuality.GOOD, Instant.now()),
                        new TelemetryPointDto("VIBRATION_RMS", 5.2, "mm/s", TelemetryQuality.GOOD, Instant.now()), // Exceeds 4.5 ISO limit!
                        new TelemetryPointDto("BEARING_TEMP", 82.5, "°C", TelemetryQuality.GOOD, Instant.now())   // Exceeds 80.0 limit!
                )
        );

        TelemetryIngestResponse response = telemetryService.ingestBatch(request, testUser);

        assertThat(response).isNotNull();
        assertThat(response.getIngestedCount()).isEqualTo(3);
        assertThat(response.getStatus()).isEqualTo("ALERTS_DETECTED");
        assertThat(response.getAlerts()).hasSize(2);
        assertThat(response.getAlerts().get(0)).contains("ISO 10816 Vibration Alert");
        assertThat(response.getAlerts().get(1)).contains("Thermal Warning");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MachineTelemetryPoint>> captor = ArgumentCaptor.forClass(List.class);
        verify(telemetryRepository, times(1)).saveAll(captor.capture());
        List<MachineTelemetryPoint> saved = captor.getValue();
        assertThat(saved).hasSize(3);
    }

    @Test
    @DisplayName("Live telemetry aggregation evaluates sensor values and diagnostic health score")
    void testGetLiveTelemetryAggregatesMetrics() {
        when(machineRepository.findByIdAndIsDeletedFalse(machineId)).thenReturn(Optional.of(testMachine));
        when(tagMappingRepository.findActiveMappingsByMachineId(machineId)).thenReturn(List.of());

        Instant now = Instant.now();
        MachineTelemetryPoint speedPoint = new MachineTelemetryPoint(testMachine, "SPINDLE_SPEED", 12000.0, "RPM", TelemetryQuality.GOOD, now);
        MachineTelemetryPoint vibPoint = new MachineTelemetryPoint(testMachine, "VIBRATION_RMS", 1.8, "mm/s", TelemetryQuality.GOOD, now);
        MachineTelemetryPoint curPoint = new MachineTelemetryPoint(testMachine, "MOTOR_CURRENT", 22.4, "A", TelemetryQuality.GOOD, now);
        MachineTelemetryPoint tempPoint = new MachineTelemetryPoint(testMachine, "BEARING_TEMP", 48.0, "°C", TelemetryQuality.GOOD, now);

        when(telemetryRepository.findLatestByMachineIdAndTagName(machineId, "SPINDLE_SPEED")).thenReturn(Optional.of(speedPoint));
        when(telemetryRepository.findLatestByMachineIdAndTagName(machineId, "VIBRATION_RMS")).thenReturn(Optional.of(vibPoint));
        when(telemetryRepository.findLatestByMachineIdAndTagName(machineId, "MOTOR_CURRENT")).thenReturn(Optional.of(curPoint));
        when(telemetryRepository.findLatestByMachineIdAndTagName(machineId, "BEARING_TEMP")).thenReturn(Optional.of(tempPoint));

        MachineLiveTelemetryDto live = telemetryService.getLiveTelemetry(machineId);

        assertThat(live).isNotNull();
        assertThat(live.getMachineId()).isEqualTo(machineId);
        assertThat(live.getSpindleSpeedRpm()).isEqualTo(12000.0);
        assertThat(live.getVibrationMmPerSec()).isEqualTo(1.8);
        assertThat(live.getMotorCurrentAmps()).isEqualTo(22.4);
        assertThat(live.getBearingTempCelsius()).isEqualTo(48.0);
        assertThat(live.getHealthScore()).isEqualTo(100);
        assertThat(live.getConnectionStatus()).isEqualTo("ONLINE");
    }

    @Test
    @DisplayName("Delete tag mapping soft-deletes and records audit trail")
    void testDeleteTagMappingSoftDeletes() {
        UUID mappingId = UUID.randomUUID();
        MachineTagMapping mapping = new MachineTagMapping();
        mapping.setId(mappingId);
        mapping.setMachine(testMachine);
        mapping.setTagName("LEGACY_SENSOR");
        mapping.setDeleted(false);

        when(tagMappingRepository.findById(mappingId)).thenReturn(Optional.of(mapping));

        telemetryService.deleteTagMapping(machineId, mappingId, testUser);

        assertThat(mapping.isDeleted()).isTrue();
        verify(tagMappingRepository, times(1)).save(mapping);
        verify(auditRecordingService, times(1)).record(
                eq(testUser.getId()),
                eq("TAG_MAPPING_DELETED"),
                eq("MachineTagMapping"),
                eq(mappingId),
                any(),
                isNull()
        );
    }
}
