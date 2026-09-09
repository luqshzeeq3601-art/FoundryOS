package com.factoryos.modules.telemetry.application;

import com.factoryos.common.exception.AppException;
import com.factoryos.modules.machine.domain.Machine;
import com.factoryos.modules.machine.repository.MachineRepository;
import com.factoryos.modules.telemetry.domain.*;
import com.factoryos.modules.telemetry.dto.RetentionExecutionReport;
import com.factoryos.modules.telemetry.dto.TimeSeriesBucketDto;
import com.factoryos.modules.telemetry.dto.TimeSeriesResponseDto;
import com.factoryos.modules.telemetry.repository.MachineTelemetryRepository;
import com.factoryos.modules.telemetry.repository.MachineTelemetryRollup1hRepository;
import com.factoryos.modules.telemetry.repository.MachineTelemetryRollup1mRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TelemetryDownsamplingService {

    private static final Logger log = LoggerFactory.getLogger(TelemetryDownsamplingService.class);

    private final MachineRepository machineRepository;
    private final MachineTelemetryRepository telemetryRepository;
    private final MachineTelemetryRollup1mRepository rollup1mRepository;
    private final MachineTelemetryRollup1hRepository rollup1hRepository;

    public TelemetryDownsamplingService(
            MachineRepository machineRepository,
            MachineTelemetryRepository telemetryRepository,
            MachineTelemetryRollup1mRepository rollup1mRepository,
            MachineTelemetryRollup1hRepository rollup1hRepository
    ) {
        this.machineRepository = machineRepository;
        this.telemetryRepository = telemetryRepository;
        this.rollup1mRepository = rollup1mRepository;
        this.rollup1hRepository = rollup1hRepository;
    }

    @Transactional
    public int aggregateOneMinuteRollups(UUID machineId, String tagName, Instant since) {
        Machine machine = machineRepository.findByIdAndIsDeletedFalse(machineId)
                .orElseThrow(() -> AppException.notFound("Machine not found with ID: " + machineId));

        List<MachineTelemetryPoint> points = telemetryRepository.findByMachineIdAndTimestampAfter(machineId, since);
        if (points.isEmpty()) return 0;

        // Group points by 1-minute bucket (truncated to minute)
        Map<Instant, List<MachineTelemetryPoint>> grouped = points.stream()
                .filter(p -> p.getTagName().equalsIgnoreCase(tagName))
                .collect(Collectors.groupingBy(p -> p.getTimestamp().truncatedTo(ChronoUnit.MINUTES)));

        List<MachineTelemetryRollup1m> rollupsToSave = new ArrayList<>();
        for (Map.Entry<Instant, List<MachineTelemetryPoint>> entry : grouped.entrySet()) {
            Instant bucketStart = entry.getKey();
            List<MachineTelemetryPoint> bucketPoints = entry.getValue();

            double min = bucketPoints.stream().mapToDouble(MachineTelemetryPoint::getMetricValue).min().orElse(0.0);
            double max = bucketPoints.stream().mapToDouble(MachineTelemetryPoint::getMetricValue).max().orElse(0.0);
            double avg = bucketPoints.stream().mapToDouble(MachineTelemetryPoint::getMetricValue).average().orElse(0.0);

            MachineTelemetryRollup1m rollup = new MachineTelemetryRollup1m(
                    machine,
                    tagName.toUpperCase(),
                    bucketStart,
                    min,
                    max,
                    avg,
                    bucketPoints.size(),
                    TelemetryQuality.GOOD
            );
            rollupsToSave.add(rollup);
        }

        rollup1mRepository.saveAll(rollupsToSave);
        return rollupsToSave.size();
    }

    @Transactional
    public int aggregateOneHourRollups(UUID machineId, String tagName, Instant from, Instant to) {
        Machine machine = machineRepository.findByIdAndIsDeletedFalse(machineId)
                .orElseThrow(() -> AppException.notFound("Machine not found with ID: " + machineId));

        List<MachineTelemetryRollup1m> minuteRollups = rollup1mRepository.findSeries(machineId, tagName.toUpperCase(), from, to);
        if (minuteRollups.isEmpty()) return 0;

        // Group 1-minute rollups by 1-hour bucket
        Map<Instant, List<MachineTelemetryRollup1m>> grouped = minuteRollups.stream()
                .collect(Collectors.groupingBy(r -> r.getBucketStart().truncatedTo(ChronoUnit.HOURS)));

        List<MachineTelemetryRollup1h> hourlyRollups = new ArrayList<>();
        for (Map.Entry<Instant, List<MachineTelemetryRollup1m>> entry : grouped.entrySet()) {
            Instant bucketStart = entry.getKey();
            List<MachineTelemetryRollup1m> rollups = entry.getValue();

            double min = rollups.stream().mapToDouble(MachineTelemetryRollup1m::getMinValue).min().orElse(0.0);
            double max = rollups.stream().mapToDouble(MachineTelemetryRollup1m::getMaxValue).max().orElse(0.0);
            double avg = rollups.stream().mapToDouble(MachineTelemetryRollup1m::getAvgValue).average().orElse(0.0);
            int totalSamples = rollups.stream().mapToInt(MachineTelemetryRollup1m::getSampleCount).sum();

            MachineTelemetryRollup1h hourly = new MachineTelemetryRollup1h(
                    machine,
                    tagName.toUpperCase(),
                    bucketStart,
                    min,
                    max,
                    avg,
                    totalSamples,
                    TelemetryQuality.GOOD
            );
            hourlyRollups.add(hourly);
        }

        rollup1hRepository.saveAll(hourlyRollups);
        return hourlyRollups.size();
    }

    @Transactional
    public RetentionExecutionReport executeRetentionPolicy(Duration rawRetention, Duration rollup1mRetention, Duration rollup1hRetention) {
        long start = System.currentTimeMillis();
        Instant now = Instant.now();

        Instant rawCutoff = now.minus(rawRetention != null ? rawRetention : Duration.ofDays(7));
        Instant rollup1mCutoff = now.minus(rollup1mRetention != null ? rollup1mRetention : Duration.ofDays(30));
        Instant rollup1hCutoff = now.minus(rollup1hRetention != null ? rollup1hRetention : Duration.ofDays(365));

        int rawPruned = telemetryRepository.pruneOlderThan(rawCutoff);
        int r1mPruned = rollup1mRepository.pruneOlderThan(rollup1mCutoff);
        int r1hPruned = rollup1hRepository.pruneOlderThan(rollup1hCutoff);

        long duration = System.currentTimeMillis() - start;
        log.info("Retention policy executed: {} raw points, {} 1m rollups, {} 1h rollups pruned in {}ms",
                rawPruned, r1mPruned, r1hPruned, duration);

        return new RetentionExecutionReport(rawPruned, r1mPruned, r1hPruned, duration);
    }

    @Transactional(readOnly = true)
    public TimeSeriesResponseDto getTimeSeries(
            UUID machineId,
            String tagName,
            Instant from,
            Instant to,
            DownsampleBucket requestedBucket
    ) {
        long start = System.currentTimeMillis();

        Machine machine = machineRepository.findByIdAndIsDeletedFalse(machineId)
                .orElseThrow(() -> AppException.notFound("Machine not found with ID: " + machineId));

        Instant toTime = to != null ? to : Instant.now();
        Instant fromTime = from != null ? from : toTime.minus(Duration.ofHours(1));

        if (fromTime.isAfter(toTime)) {
            throw AppException.badRequest("'from' timestamp must be before 'to' timestamp");
        }

        Duration span = Duration.between(fromTime, toTime);

        // Auto-select resolution if not specified
        DownsampleBucket resolution = requestedBucket;
        if (resolution == null) {
            if (span.toHours() <= 2) {
                resolution = DownsampleBucket.ONE_SECOND;
            } else if (span.toDays() <= 7) {
                resolution = DownsampleBucket.ONE_MINUTE;
            } else {
                resolution = DownsampleBucket.ONE_HOUR;
            }
        }

        List<TimeSeriesBucketDto> seriesPoints = new ArrayList<>();
        String unit = deriveUnit(tagName);

        if (resolution == DownsampleBucket.ONE_SECOND) {
            List<MachineTelemetryPoint> rawPoints = telemetryRepository.findSeries(machineId, tagName.toUpperCase(), fromTime, toTime);
            for (MachineTelemetryPoint pt : rawPoints) {
                seriesPoints.add(new TimeSeriesBucketDto(
                        pt.getTimestamp(),
                        pt.getMetricValue(),
                        pt.getMetricValue(),
                        pt.getMetricValue(),
                        1,
                        pt.getQuality().name()
                ));
            }
        } else if (resolution == DownsampleBucket.ONE_MINUTE) {
            List<MachineTelemetryRollup1m> rollups = rollup1mRepository.findSeries(machineId, tagName.toUpperCase(), fromTime, toTime);
            for (MachineTelemetryRollup1m r : rollups) {
                seriesPoints.add(new TimeSeriesBucketDto(
                        r.getBucketStart(),
                        r.getAvgValue(),
                        r.getMinValue(),
                        r.getMaxValue(),
                        r.getSampleCount(),
                        r.getQuality().name()
                ));
            }
        } else {
            List<MachineTelemetryRollup1h> rollups = rollup1hRepository.findSeries(machineId, tagName.toUpperCase(), fromTime, toTime);
            for (MachineTelemetryRollup1h r : rollups) {
                seriesPoints.add(new TimeSeriesBucketDto(
                        r.getBucketStart(),
                        r.getAvgValue(),
                        r.getMinValue(),
                        r.getMaxValue(),
                        r.getSampleCount(),
                        r.getQuality().name()
                ));
            }
        }

        long executionMs = System.currentTimeMillis() - start;

        TimeSeriesResponseDto response = new TimeSeriesResponseDto();
        response.setMachineId(machine.getId());
        response.setMachineName(machine.getName());
        response.setTagName(tagName.toUpperCase());
        response.setUnit(unit);
        response.setBucketResolution(resolution.getCode());
        response.setFrom(fromTime);
        response.setTo(toTime);
        response.setPointCount(seriesPoints.size());
        response.setQueryExecutionMs(executionMs);
        response.setSeries(seriesPoints);

        return response;
    }

    private String deriveUnit(String tagName) {
        String upper = tagName.toUpperCase();
        if (upper.contains("SPEED")) return "RPM";
        if (upper.contains("VIBRATION")) return "mm/s";
        if (upper.contains("CURRENT")) return "A";
        if (upper.contains("TEMP")) return "°C";
        return "";
    }
}
