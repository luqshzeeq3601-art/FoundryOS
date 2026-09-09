package com.factoryos.modules.telemetry.application;

import com.factoryos.modules.machine.domain.Machine;
import com.factoryos.modules.machine.domain.MachineStatus;
import com.factoryos.modules.machine.repository.MachineRepository;
import com.factoryos.modules.telemetry.domain.DownsampleBucket;
import com.factoryos.modules.telemetry.domain.MachineTelemetryRollup1h;
import com.factoryos.modules.telemetry.domain.TelemetryQuality;
import com.factoryos.modules.telemetry.dto.TimeSeriesResponseDto;
import com.factoryos.modules.telemetry.repository.MachineTelemetryRepository;
import com.factoryos.modules.telemetry.repository.MachineTelemetryRollup1hRepository;
import com.factoryos.modules.telemetry.repository.MachineTelemetryRollup1mRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelemetryDownsamplingPerformanceTest {

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
    private List<MachineTelemetryRollup1h> mockThirtyDayHourlyRollups;

    @BeforeEach
    void setUp() {
        machineId = UUID.randomUUID();
        testMachine = new Machine();
        testMachine.setId(machineId);
        testMachine.setName("CNC Mill #01");
        testMachine.setSerialNumber("CNC-2026-001");
        testMachine.setStatus(MachineStatus.RUNNING);

        // Pre-generate 30 days of hourly rollup records (24 * 30 = 720 records)
        int hoursIn30Days = 30 * 24;
        mockThirtyDayHourlyRollups = new ArrayList<>(hoursIn30Days);
        Instant baseTime = Instant.now().truncatedTo(ChronoUnit.HOURS).minus(Duration.ofDays(30));

        for (int i = 0; i < hoursIn30Days; i++) {
            Instant bucket = baseTime.plus(Duration.ofHours(i));
            mockThirtyDayHourlyRollups.add(new MachineTelemetryRollup1h(
                    testMachine,
                    "SPINDLE_SPEED",
                    bucket,
                    7800.0,
                    12500.0,
                    9450.0,
                    3600, // 3600 seconds of raw points aggregated into this hour
                    TelemetryQuality.GOOD
            ));
        }
    }

    @Test
    @DisplayName("Performance SLA: 30-day downsampled series query executes in under 200 ms")
    void testThirtyDayQueryPerformanceUnder200Ms() {
        when(machineRepository.findByIdAndIsDeletedFalse(machineId)).thenReturn(Optional.of(testMachine));
        when(rollup1hRepository.findSeries(eq(machineId), eq("SPINDLE_SPEED"), any(), any()))
                .thenReturn(mockThirtyDayHourlyRollups);

        Instant to = Instant.now();
        Instant from = to.minus(Duration.ofDays(30));

        long startTime = System.nanoTime();
        TimeSeriesResponseDto response = downsamplingService.getTimeSeries(
                machineId,
                "SPINDLE_SPEED",
                from,
                to,
                DownsampleBucket.ONE_HOUR
        );
        long elapsedNanos = System.nanoTime() - startTime;
        double elapsedMs = elapsedNanos / 1_000_000.0;

        // Verify dataset correctness
        assertThat(response).isNotNull();
        assertThat(response.getBucketResolution()).isEqualTo("1h");
        assertThat(response.getPointCount()).isEqualTo(720);
        assertThat(response.getSeries()).hasSize(720);
        assertThat(response.getSeries().get(0).getAvg()).isEqualTo(9450.0);
        assertThat(response.getSeries().get(0).getCount()).isEqualTo(3600);

        // Verify strict SLA target: under 200 ms
        assertThat(elapsedMs)
                .as("30-day query elapsed time must be under 200 ms")
                .isLessThan(200.0);

        // Also check internal measured queryExecutionMs
        assertThat(response.getQueryExecutionMs()).isLessThan(200L);
    }

    @Test
    @DisplayName("High-concurrency burst benchmark: 50 consecutive 30-day queries average < 50 ms")
    void testConsecutiveQueryLatencyBenchmark() {
        when(machineRepository.findByIdAndIsDeletedFalse(machineId)).thenReturn(Optional.of(testMachine));
        when(rollup1hRepository.findSeries(eq(machineId), eq("SPINDLE_SPEED"), any(), any()))
                .thenReturn(mockThirtyDayHourlyRollups);

        Instant to = Instant.now();
        Instant from = to.minus(Duration.ofDays(30));

        int iterations = 50;
        long totalElapsedNanos = 0;

        // Warm-up run
        downsamplingService.getTimeSeries(machineId, "SPINDLE_SPEED", from, to, DownsampleBucket.ONE_HOUR);

        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            TimeSeriesResponseDto res = downsamplingService.getTimeSeries(
                    machineId,
                    "SPINDLE_SPEED",
                    from,
                    to,
                    DownsampleBucket.ONE_HOUR
            );
            totalElapsedNanos += (System.nanoTime() - start);
            assertThat(res.getPointCount()).isEqualTo(720);
        }

        double avgLatencyMs = (totalElapsedNanos / (double) iterations) / 1_000_000.0;
        assertThat(avgLatencyMs)
                .as("Average query latency over 50 iterations must be under 50 ms")
                .isLessThan(50.0);
    }
}
