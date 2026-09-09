package com.factoryos.modules.telemetry.application;

import com.factoryos.common.exception.AppException;
import com.factoryos.modules.audit.application.AuditRecordingService;
import com.factoryos.modules.auth.domain.User;
import com.factoryos.modules.machine.domain.Machine;
import com.factoryos.modules.machine.repository.MachineRepository;
import com.factoryos.modules.telemetry.domain.MachineTagMapping;
import com.factoryos.modules.telemetry.domain.MachineTelemetryPoint;
import com.factoryos.modules.telemetry.domain.ProtocolType;
import com.factoryos.modules.telemetry.domain.TelemetryQuality;
import com.factoryos.modules.telemetry.dto.*;
import com.factoryos.modules.telemetry.repository.MachineTagMappingRepository;
import com.factoryos.modules.telemetry.repository.MachineTelemetryRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TelemetryIngestionService {

    private final MachineRepository machineRepository;
    private final MachineTagMappingRepository tagMappingRepository;
    private final MachineTelemetryRepository telemetryRepository;
    private final AuditRecordingService auditRecordingService;

    public TelemetryIngestionService(
            MachineRepository machineRepository,
            MachineTagMappingRepository tagMappingRepository,
            MachineTelemetryRepository telemetryRepository,
            AuditRecordingService auditRecordingService
    ) {
        this.machineRepository = machineRepository;
        this.tagMappingRepository = tagMappingRepository;
        this.telemetryRepository = telemetryRepository;
        this.auditRecordingService = auditRecordingService;
    }

    @Transactional
    public TagMappingDto createTagMapping(UUID machineId, CreateTagMappingRequest request, User actor) {
        Machine machine = machineRepository.findByIdAndIsDeletedFalse(machineId)
                .orElseThrow(() -> AppException.notFound("Machine not found with ID: " + machineId));

        String normalizedTag = request.getTagName().trim().toUpperCase();
        if (tagMappingRepository.existsByMachineIdAndTagNameAndIsDeletedFalse(machineId, normalizedTag)) {
            throw AppException.conflict("DUPLICATE_TAG", "A tag mapping for '" + normalizedTag + "' already exists on machine " + machine.getName());
        }

        MachineTagMapping mapping = new MachineTagMapping();
        mapping.setMachine(machine);
        mapping.setTagName(normalizedTag);
        mapping.setProtocol(request.getProtocol() != null ? request.getProtocol() : ProtocolType.OPC_UA);
        mapping.setTagAddress(request.getTagAddress().trim());
        mapping.setDataType(request.getDataType() != null ? request.getDataType().trim() : "DOUBLE");
        mapping.setUnitOfMeasure(request.getUnitOfMeasure() != null ? request.getUnitOfMeasure().trim() : null);
        mapping.setScaleFactor(request.getScaleFactor() != null ? request.getScaleFactor() : BigDecimal.ONE);
        mapping.setActive(true);
        mapping.setCreatedBy(actor != null ? actor.getId() : null);
        mapping.setUpdatedBy(actor != null ? actor.getId() : null);

        MachineTagMapping saved = tagMappingRepository.save(mapping);

        auditRecordingService.record(
                actor != null ? actor.getId() : null,
                "TAG_MAPPING_CREATED",
                "MachineTagMapping",
                saved.getId(),
                null,
                Map.of("machineId", machineId.toString(), "tagName", normalizedTag, "protocol", saved.getProtocol().name())
        );

        return TagMappingDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<TagMappingDto> getTagMappings(UUID machineId) {
        if (machineRepository.findByIdAndIsDeletedFalse(machineId).isEmpty()) {
            throw AppException.notFound("Machine not found with ID: " + machineId);
        }
        return tagMappingRepository.findByMachineIdAndIsDeletedFalse(machineId)
                .stream()
                .map(TagMappingDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteTagMapping(UUID machineId, UUID mappingId, User actor) {
        MachineTagMapping mapping = tagMappingRepository.findById(mappingId)
                .filter(m -> !m.isDeleted() && m.getMachine().getId().equals(machineId))
                .orElseThrow(() -> AppException.notFound("Tag mapping not found with ID: " + mappingId));

        mapping.setDeleted(true);
        mapping.setUpdatedBy(actor != null ? actor.getId() : null);
        tagMappingRepository.save(mapping);

        auditRecordingService.record(
                actor != null ? actor.getId() : null,
                "TAG_MAPPING_DELETED",
                "MachineTagMapping",
                mappingId,
                Map.of("machineId", machineId.toString(), "tagName", mapping.getTagName()),
                null
        );
    }

    @Transactional
    public TelemetryIngestResponse ingestBatch(TelemetryBatchIngestRequest request, User actor) {
        UUID machineId = request.getMachineId();
        Machine machine = machineRepository.findByIdAndIsDeletedFalse(machineId)
                .orElseThrow(() -> AppException.notFound("Machine not found with ID: " + machineId));

        List<MachineTagMapping> mappings = tagMappingRepository.findActiveMappingsByMachineId(machineId);
        Map<String, MachineTagMapping> mappingMap = mappings.stream()
                .collect(Collectors.toMap(m -> m.getTagName().toUpperCase(), m -> m, (a, b) -> a));

        List<MachineTelemetryPoint> pointsToSave = new ArrayList<>();
        List<String> alerts = new ArrayList<>();
        Instant now = Instant.now();

        for (TelemetryPointDto pt : request.getPoints()) {
            String tagName = pt.getTagName().trim().toUpperCase();
            double rawVal = pt.getValue();

            // Apply scale factor if tag mapping exists
            double scaledVal = rawVal;
            String unit = pt.getUnit();
            if (mappingMap.containsKey(tagName)) {
                MachineTagMapping map = mappingMap.get(tagName);
                scaledVal = rawVal * map.getScaleFactor().doubleValue();
                if (unit == null || unit.isBlank()) {
                    unit = map.getUnitOfMeasure();
                }
            }

            // Anomaly & ISO Threshold Checks
            if (tagName.contains("VIBRATION") && scaledVal > 4.5) {
                alerts.add("ISO 10816 Vibration Alert on " + machine.getName() + ": " + String.format("%.2f", scaledVal) + " mm/s exceeds limit (4.5 mm/s)");
            }
            if (tagName.contains("TEMP") && scaledVal > 80.0) {
                alerts.add("Thermal Warning on " + machine.getName() + ": " + String.format("%.1f", scaledVal) + "°C exceeds threshold (80.0°C)");
            }
            if (tagName.contains("CURRENT") && scaledVal > 45.0) {
                alerts.add("Overcurrent Warning on " + machine.getName() + ": " + String.format("%.1f", scaledVal) + " A exceeds rating (45.0 A)");
            }

            Instant ptTimestamp = pt.getTimestamp() != null ? pt.getTimestamp() : now;
            // Reject timestamps older than 7 days or more than 5 minutes in the future
            if (ptTimestamp.isBefore(now.minus(Duration.ofDays(7))) || ptTimestamp.isAfter(now.plus(Duration.ofMinutes(5)))) {
                ptTimestamp = now;
            }

            MachineTelemetryPoint point = new MachineTelemetryPoint(
                    machine,
                    tagName,
                    scaledVal,
                    unit,
                    pt.getQuality() != null ? pt.getQuality() : TelemetryQuality.GOOD,
                    ptTimestamp
            );
            pointsToSave.add(point);
        }

        telemetryRepository.saveAll(pointsToSave);

        return new TelemetryIngestResponse(
                machineId,
                pointsToSave.size(),
                alerts.isEmpty() ? "SUCCESS" : "ALERTS_DETECTED",
                alerts
        );
    }

    @Transactional(readOnly = true)
    public MachineLiveTelemetryDto getLiveTelemetry(UUID machineId) {
        Machine machine = machineRepository.findByIdAndIsDeletedFalse(machineId)
                .orElseThrow(() -> AppException.notFound("Machine not found with ID: " + machineId));

        List<MachineTagMapping> mappings = tagMappingRepository.findActiveMappingsByMachineId(machineId);

        MachineLiveTelemetryDto dto = new MachineLiveTelemetryDto();
        dto.setMachineId(machine.getId());
        dto.setMachineName(machine.getName());
        dto.setSerialNumber(machine.getSerialNumber());
        dto.setMachineStatus(machine.getStatus());
        dto.setConfiguredTagsCount(mappings.size());

        ProtocolType activeProtocol = mappings.isEmpty() ? ProtocolType.OPC_UA : mappings.get(0).getProtocol();
        dto.setActiveProtocol(activeProtocol);

        // Fetch latest metrics
        Optional<MachineTelemetryPoint> speedPt = telemetryRepository.findLatestByMachineIdAndTagName(machineId, "SPINDLE_SPEED");
        Optional<MachineTelemetryPoint> vibPt = telemetryRepository.findLatestByMachineIdAndTagName(machineId, "VIBRATION_RMS");
        Optional<MachineTelemetryPoint> curPt = telemetryRepository.findLatestByMachineIdAndTagName(machineId, "MOTOR_CURRENT");
        Optional<MachineTelemetryPoint> tempPt = telemetryRepository.findLatestByMachineIdAndTagName(machineId, "BEARING_TEMP");

        double speed = speedPt.map(MachineTelemetryPoint::getMetricValue).orElse(0.0);
        double vib = vibPt.map(MachineTelemetryPoint::getMetricValue).orElse(0.0);
        double current = curPt.map(MachineTelemetryPoint::getMetricValue).orElse(0.0);
        double temp = tempPt.map(MachineTelemetryPoint::getMetricValue).orElse(24.0); // Ambient baseline

        dto.setSpindleSpeedRpm(speed);
        dto.setVibrationMmPerSec(vib);
        dto.setMotorCurrentAmps(current);
        dto.setBearingTempCelsius(temp);

        // Determine heartbeat & connection status
        Instant latestTime = speedPt.map(MachineTelemetryPoint::getTimestamp).orElse(
                vibPt.map(MachineTelemetryPoint::getTimestamp).orElse(
                        tempPt.map(MachineTelemetryPoint::getTimestamp).orElse(null)
                )
        );
        dto.setLastHeartbeat(latestTime);

        int health = 100;
        if (vib > 4.5) health -= 35;
        else if (vib > 2.8) health -= 15;

        if (temp > 80.0) health -= 40;
        else if (temp > 65.0) health -= 15;

        if (current > 45.0) health -= 25;
        health = Math.max(0, Math.min(100, health));
        dto.setHealthScore(health);

        if (latestTime == null || latestTime.isBefore(Instant.now().minus(Duration.ofMinutes(15)))) {
            dto.setConnectionStatus("OFFLINE");
        } else if (health < 50) {
            dto.setConnectionStatus("CRITICAL");
        } else if (health < 80) {
            dto.setConnectionStatus("WARNING");
        } else {
            dto.setConnectionStatus("ONLINE");
        }

        return dto;
    }

    @Transactional(readOnly = true)
    public List<TelemetryPointDto> getTelemetryHistory(UUID machineId, int limit) {
        if (machineRepository.findByIdAndIsDeletedFalse(machineId).isEmpty()) {
            throw AppException.notFound("Machine not found with ID: " + machineId);
        }

        int pageSize = Math.min(Math.max(limit, 1), 200);
        return telemetryRepository.findByMachineIdOrderByTimestampDesc(machineId, PageRequest.of(0, pageSize))
                .stream()
                .map(pt -> new TelemetryPointDto(
                        pt.getTagName(),
                        pt.getMetricValue(),
                        pt.getMetricUnit(),
                        pt.getQuality(),
                        pt.getTimestamp()
                ))
                .collect(Collectors.toList());
    }
}
