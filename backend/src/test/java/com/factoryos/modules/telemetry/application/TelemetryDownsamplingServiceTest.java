package com.factoryos.modules.telemetry.application;

import com.factoryos.modules.machine.domain.Machine;
import com.factoryos.modules.machine.domain.MachineStatus;
import com.factoryos.modules.machine.repository.MachineRepository;
import com.factoryos.modules.telemetry.domain.*;
import com.factoryos.modules.telemetry.dto.RetentionExecutionReport;
import com.factoryos.modules.telemetry.dto.TimeSeriesResponseDto;
import com.factoryos.modules.telemetry.repository.MachineTelemetryRepository;
import com.factoryos.modules.telemetry.repository.MachineTelemetryRollup1hRepository;
import com.factoryos.modules.telemetry.repository.MachineTelemetryRollup1mRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelemetryDownsamplingServiceTest {

    @Mock
    private MachineRepository machineRepository;

    @Mock
    private MachineTelemetryRepository telemetryRepository;

    @Mock
    private MachineTelemetryRollup1mRepository rollup1mRepository;

    @Mock
    private MachineTelemetryRollup1hRepository rollup1hRepository;

    @InjectMocks
    private TelemetryDownsamplingService downsamplingService;

    private Machine testMachine;
    private UUID machineId;

    @BeforeEach
    void setUp() {
        machineId = UUID.randomUUID();
        testMachine = new Machine();
        testMachine.setId(machineId);
        testMachine.setName("Precision Lathe #02");
        testMachine.setSerialNumber("LATHE-2026-002");
        testMachine.setStatus(MachineStatus.RUNNING);
    }

    @Test
    @DisplayName("Aggregate raw points into 1-minute rollup bucket calculating min, max, avg, count")
    void testAggregateOneMinuteRollups() {
        Instant minuteStart = Instant.now().truncatedTo(ChronoUnit.MINUTES).minus(Duration.ofMinutes(2));
        MachineTelemetryPoint p1 = new MachineTelemetryPoint(testMachine, "SPINDLE_SPEED", 8000.0, "RPM", TelemetryQuality.GOOD, minuteStart.plusSeconds(5));
        MachineTelemetryPoint p2 = new MachineTelemetryPoint(testMachine, "SPINDLE_SPEED", 9500.0, "RPM", TelemetryQuality.GOOD, minuteStart.plusSeconds(25));
        MachineTelemetryPoint p3 = new MachineTelemetryPoint(testMachine, "SPINDLE_SPEED", 8600.0, "RPM", TelemetryQuality.GOOD, minuteStart.plusSeconds(50));

        when(machineRepository.findByIdAndIsDeletedFalse(machineId)).thenReturn(Optional.of(testMachine));
        when(telemetryRepository.findByMachineIdAndTimestampAfter(eq(machineId), any(Instant.class)))
                .thenReturn(List.of(p1, p2, p3));

        int created = downsamplingService.aggregateOneMinuteRollups(machineId, "SPINDLE_SPEED", minuteStart.minusSeconds(10));

        assertThat(created).isEqualTo(1);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MachineTelemetryRollup1m>> captor = ArgumentCaptor.forClass(List.class);
        verify(rollup1mRepository, times(1)).saveAll(captor.capture());
        List<MachineTelemetryRollup1m> saved = captor.getValue();
        assertThat(saved).hasSize(1);
        MachineTelemetryRollup1m rollup = saved.get(0);

        assertThat(rollup.getMinValue()).isEqualTo(8000.0);
        assertThat(rollup.getMaxValue()).isEqualTo(9500.0);
        assertThat(rollup.getAvgValue()).isEqualTo((8000.0 + 9500.0 + 8600.0) / 3.0);
        assertThat(rollup.getSampleCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("Aggregate 1-minute rollups into 1-hour rollup bucket")
    void testAggregateOneHourRollups() {
        Instant hourStart = Instant.now().truncatedTo(ChronoUnit.HOURS).minus(Duration.ofHours(1));

        MachineTelemetryRollup1m r1 = new MachineTelemetryRollup1m(testMachine, "VIBRATION_RMS", hourStart.plus(Duration.ofMinutes(10)), 1.2, 2.5, 1.8, 60, TelemetryQuality.GOOD);
        MachineTelemetryRollup1m r2 = new MachineTelemetryRollup1m(testMachine, "VIBRATION_RMS", hourStart.plus(Duration.ofMinutes(30)), 1.5, 3.2, 2.1, 60, TelemetryQuality.GOOD);

        when(machineRepository.findByIdAndIsDeletedFalse(machineId)).thenReturn(Optional.of(testMachine));
        when(rollup1mRepository.findSeries(eq(machineId), eq("VIBRATION_RMS"), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(r1, r2));

        int created = downsamplingService.aggregateOneHourRollups(machineId, "VIBRATION_RMS", hourStart, hourStart.plus(Duration.ofHours(1)));

        assertThat(created).isEqualTo(1);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MachineTelemetryRollup1h>> captor = ArgumentCaptor.forClass(List.class);
        verify(rollup1hRepository, times(1)).saveAll(captor.capture());
        List<MachineTelemetryRollup1h> saved = captor.getValue();
        assertThat(saved).hasSize(1);
        MachineTelemetryRollup1h hourly = saved.get(0);

        assertThat(hourly.getMinValue()).isEqualTo(1.2);
        assertThat(hourly.getMaxValue()).isEqualTo(3.2);
        assertThat(hourly.getAvgValue()).isCloseTo(1.95, org.assertj.core.data.Offset.offset(0.001));
        assertThat(hourly.getSampleCount()).isEqualTo(120);
    }

    @Test
    @DisplayName("Retention policy execution prunes raw points and rollups past cutoff")
    void testRetentionPolicyExecution() {
        when(telemetryRepository.pruneOlderThan(any(Instant.class))).thenReturn(15000);
        when(rollup1mRepository.pruneOlderThan(any(Instant.class))).thenReturn(1200);
        when(rollup1hRepository.pruneOlderThan(any(Instant.class))).thenReturn(48);

        RetentionExecutionReport report = downsamplingService.executeRetentionPolicy(
                Duration.ofDays(7),
                Duration.ofDays(30),
                Duration.ofDays(365)
        );

        assertThat(report).isNotNull();
        assertThat(report.getRawPointsPruned()).isEqualTo(15000);
        assertThat(report.getRollups1mPruned()).isEqualTo(1200);
        assertThat(report.getRollups1hPruned()).isEqualTo(48);
        assertThat(report.getExecutionTimeMs()).isGreaterThanOrEqualTo(0L);

        verify(telemetryRepository, times(1)).pruneOlderThan(any(Instant.class));
        verify(rollup1mRepository, times(1)).pruneOlderThan(any(Instant.class));
        verify(rollup1hRepository, times(1)).pruneOlderThan(any(Instant.class));
    }

    @Test
    @DisplayName("Automatic bucket resolution chooses 1s for short range, 1m for multi-day, 1h for month")
    void testAutomaticBucketResolution() {
        when(machineRepository.findByIdAndIsDeletedFalse(machineId)).thenReturn(Optional.of(testMachine));

        Instant now = Instant.now();

        // 1. 1 hour span -> 1s raw
        when(telemetryRepository.findSeries(eq(machineId), eq("BEARING_TEMP"), any(), any()))
                .thenReturn(List.of());
        TimeSeriesResponseDto shortRange = downsamplingService.getTimeSeries(machineId, "BEARING_TEMP", now.minus(Duration.ofHours(1)), now, null);
        assertThat(shortRange.getBucketResolution()).isEqualTo("1s");

        // 2. 3 days span -> 1m rollups
        when(rollup1mRepository.findSeries(eq(machineId), eq("BEARING_TEMP"), any(), any()))
                .thenReturn(List.of());
        TimeSeriesResponseDto midRange = downsamplingService.getTimeSeries(machineId, "BEARING_TEMP", now.minus(Duration.ofDays(3)), now, null);
        assertThat(midRange.getBucketResolution()).isEqualTo("1m");

        // 3. 30 days span -> 1h rollups
        when(rollup1hRepository.findSeries(eq(machineId), eq("BEARING_TEMP"), any(), any()))
                .thenReturn(List.of());
        TimeSeriesResponseDto longRange = downsamplingService.getTimeSeries(machineId, "BEARING_TEMP", now.minus(Duration.ofDays(30)), now, null);
        assertThat(longRange.getBucketResolution()).isEqualTo("1h");
    }
}
