package com.factoryos.modules.downtime.application;

import com.factoryos.common.exception.AppException;
import com.factoryos.modules.audit.application.AuditRecordingService;
import com.factoryos.modules.auth.domain.User;
import com.factoryos.modules.downtime.domain.DowntimeEvent;
import com.factoryos.modules.downtime.domain.DowntimeReasonCode;
import com.factoryos.modules.downtime.domain.DowntimeTriggerSource;
import com.factoryos.modules.downtime.dto.AcknowledgeRootCauseRequest;
import com.factoryos.modules.downtime.dto.AutomatedEvaluationResultDto;
import com.factoryos.modules.downtime.dto.DowntimeEventDto;
import com.factoryos.modules.downtime.dto.MicroStopSummaryDto;
import com.factoryos.modules.downtime.repository.DowntimeEventRepository;
import com.factoryos.modules.machine.domain.Machine;
import com.factoryos.modules.machine.domain.MachineStatus;
import com.factoryos.modules.machine.repository.MachineRepository;
import com.factoryos.modules.production.domain.ProductionOrder;
import com.factoryos.modules.production.domain.ProductionOrderStatus;
import com.factoryos.modules.production.repository.ProductionOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AutomatedDowntimeDetectionService {

    public static final long MICRO_STOP_THRESHOLD_SECONDS = 180L; // 3 minutes

    private final DowntimeEventRepository downtimeEventRepository;
    private final MachineRepository machineRepository;
    private final ProductionOrderRepository productionOrderRepository;
    private final AuditRecordingService auditRecordingService;

    public AutomatedDowntimeDetectionService(
            DowntimeEventRepository downtimeEventRepository,
            MachineRepository machineRepository,
            ProductionOrderRepository productionOrderRepository,
            AuditRecordingService auditRecordingService
    ) {
        this.downtimeEventRepository = downtimeEventRepository;
        this.machineRepository = machineRepository;
        this.productionOrderRepository = productionOrderRepository;
        this.auditRecordingService = auditRecordingService;
    }

    @Transactional
    public AutomatedEvaluationResultDto evaluateMachineStream(
            UUID machineId,
            double spindleSpeed,
            double cycleCountDelta,
            Instant eventTime
    ) {
        Machine machine = machineRepository.findByIdAndIsDeletedFalse(machineId)
                .orElseThrow(() -> AppException.notFound("Machine not found with ID: " + machineId));

        Instant now = eventTime != null ? eventTime : Instant.now();

        // Check if machine has an active in-progress production order
        Optional<ProductionOrder> activeOrderOpt = productionOrderRepository
                .findByMachineIdAndStatus(machineId, ProductionOrderStatus.IN_PROGRESS);

        if (activeOrderOpt.isEmpty()) {
            // No active production order — machine stopped state is considered planned idle
            return new AutomatedEvaluationResultDto(machineId, "NO_ACTIVE_ORDER", null, "No active order on asset");
        }

        ProductionOrder activeOrder = activeOrderOpt.get();
        boolean isStopped = spindleSpeed <= 0.0 && cycleCountDelta <= 0.0;
        Optional<DowntimeEvent> openEventOpt = downtimeEventRepository.findByMachineIdAndEndTimeIsNull(machineId);

        // Case 1: Machine is currently RUNNING/IDLE and zero-pulse / stopped is detected
        if (isStopped && openEventOpt.isEmpty()) {
            DowntimeEvent event = new DowntimeEvent();
            event.setMachine(machine);
            event.setReasonCode(DowntimeReasonCode.MICRO_STOP);
            event.setTriggerSource(DowntimeTriggerSource.AUTOMATED_SENSOR);
            event.setDescription("Automated sensor stop detected during active order " + activeOrder.getOrderNumber());
            event.setStartTime(now);
            event.setMicroStop(false);

            DowntimeEvent saved = downtimeEventRepository.save(event);

            if (machine.getStatus() != MachineStatus.DOWN) {
                machine.setStatus(MachineStatus.DOWN);
                machine.setUpdatedAt(now);
                machineRepository.save(machine);
            }

            auditRecordingService.record(
                    null,
                    "AUTOMATED_DOWNTIME_TRIGGERED",
                    "DowntimeEvent",
                    saved.getId(),
                    null,
                    Map.of(
                            "machineId", machineId.toString(),
                            "machineName", machine.getName(),
                            "triggerSource", "AUTOMATED_SENSOR",
                            "orderNumber", activeOrder.getOrderNumber()
                    )
            );

            return new AutomatedEvaluationResultDto(
                    machineId,
                    "TRIGGERED_DOWN",
                    saved.getId(),
                    "Machine automatically transitioned to DOWN due to zero sensor pulse"
            );
        }

        // Case 2: Machine is already DOWN with an open downtime event
        if (openEventOpt.isPresent()) {
            DowntimeEvent openEvent = openEventOpt.get();
            long durationSeconds = Math.max(0, Duration.between(openEvent.getStartTime(), now).toSeconds());

            // Check if machine has resumed cycling
            boolean hasResumed = spindleSpeed > 0.0 || cycleCountDelta > 0.0;

            if (hasResumed) {
                // Stoppage lasted < 180 seconds: Automatic Micro-Stop Resolution
                if (durationSeconds < MICRO_STOP_THRESHOLD_SECONDS) {
                    openEvent.setEndTime(now);
                    openEvent.setResolutionNote("Auto-resolved micro-stop (" + durationSeconds + "s)");
                    openEvent.setMicroStop(true);
                    openEvent.setResolvedBy(null);
                    openEvent.setUpdatedAt(now);
                    downtimeEventRepository.save(openEvent);

                    machine.setStatus(MachineStatus.RUNNING);
                    machine.setUpdatedAt(now);
                    machineRepository.save(machine);

                    auditRecordingService.record(
                            null,
                            "MICRO_STOP_RESOLVED",
                            "DowntimeEvent",
                            openEvent.getId(),
                            Map.of("status", "OPEN"),
                            Map.of(
                                    "durationSeconds", String.valueOf(durationSeconds),
                                    "isMicroStop", "true",
                                    "machineName", machine.getName()
                            )
                    );

                    return new AutomatedEvaluationResultDto(
                            machineId,
                            "RESOLVED_MICRO_STOP",
                            openEvent.getId(),
                            "Micro-stop auto-resolved in " + durationSeconds + "s"
                    );
                } else {
                    // Stoppage exceeded 180 seconds (Major breakdown):
                    // If operator already acknowledged root cause, auto-close the downtime event upon resume
                    if (openEvent.getRootCauseAcknowledgedAt() != null) {
                        openEvent.setEndTime(now);
                        if (openEvent.getResolutionNote() == null) {
                            openEvent.setResolutionNote("Resolved after operator root cause acknowledgment");
                        }
                        openEvent.setMicroStop(false);
                        openEvent.setUpdatedAt(now);
                        downtimeEventRepository.save(openEvent);

                        machine.setStatus(MachineStatus.RUNNING);
                        machine.setUpdatedAt(now);
                        machineRepository.save(machine);

                        auditRecordingService.record(
                                null,
                                "DOWNTIME_RESOLVED_AFTER_ACK",
                                "DowntimeEvent",
                                openEvent.getId(),
                                Map.of("status", "OPEN"),
                                Map.of(
                                        "durationSeconds", String.valueOf(durationSeconds),
                                        "reasonCode", openEvent.getReasonCode().name()
                                )
                        );

                        return new AutomatedEvaluationResultDto(
                                machineId,
                                "RESOLVED_AFTER_ACK",
                                openEvent.getId(),
                                "Downtime resolved after operator root cause acknowledgment"
                        );
                    } else {
                        // Stoppage exceeded 180 seconds and root cause prompt has not been acknowledged
                        if (openEvent.getRootCausePromptedAt() == null) {
                            openEvent.setRootCausePromptedAt(now);
                            openEvent.setUpdatedAt(now);
                            downtimeEventRepository.save(openEvent);
                        }
                        return new AutomatedEvaluationResultDto(
                                machineId,
                                "FLAGGED_OPERATOR_PROMPT",
                                openEvent.getId(),
                                "Downtime exceeded 180s; awaiting operator root cause acknowledgment"
                        );
                    }
                }
            } else {
                // Still stopped: Check if we crossed the 180-second threshold to flag the operator prompt
                if (durationSeconds >= MICRO_STOP_THRESHOLD_SECONDS && openEvent.getRootCausePromptedAt() == null) {
                    openEvent.setRootCausePromptedAt(now);
                    openEvent.setUpdatedAt(now);
                    downtimeEventRepository.save(openEvent);

                    auditRecordingService.record(
                            null,
                            "OPERATOR_ROOT_CAUSE_PROMPTED",
                            "DowntimeEvent",
                            openEvent.getId(),
                            null,
                            Map.of(
                                    "machineId", machineId.toString(),
                                    "durationSeconds", String.valueOf(durationSeconds),
                                    "promptTime", now.toString()
                            )
                    );

                    return new AutomatedEvaluationResultDto(
                            machineId,
                            "FLAGGED_OPERATOR_PROMPT",
                            openEvent.getId(),
                            "Downtime crossed 180s threshold; operator root cause prompt triggered"
                    );
                }
            }
        }

        return new AutomatedEvaluationResultDto(machineId, "NONE", null, "No state change required");
    }

    @Transactional
    public DowntimeEventDto acknowledgeRootCause(
            UUID downtimeId,
            AcknowledgeRootCauseRequest request,
            User actor
    ) {
        DowntimeEvent event = downtimeEventRepository.findById(downtimeId)
                .filter(e -> !e.isDeleted())
                .orElseThrow(() -> AppException.notFound("Downtime event not found with ID: " + downtimeId));

        if (event.getEndTime() != null) {
            throw AppException.conflict("ALREADY_RESOLVED", "Downtime event has already been resolved");
        }

        if (request.getExpectedVersion() == null || !request.getExpectedVersion().equals(event.getVersion())) {
            throw AppException.versionConflict("Downtime event modified by another user. Reload and retry.");
        }

        event.setReasonCode(request.getReasonCode());
        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            event.setDescription(request.getDescription().trim());
        }
        event.setRootCauseAcknowledgedAt(Instant.now());
        event.setUpdatedBy(actor != null ? actor.getId() : null);
        event.setUpdatedAt(Instant.now());

        DowntimeEvent saved = downtimeEventRepository.save(event);

        auditRecordingService.record(
                actor != null ? actor.getId() : null,
                "ROOT_CAUSE_ACKNOWLEDGED",
                "DowntimeEvent",
                saved.getId(),
                Map.of("reasonCode", "MICRO_STOP"),
                Map.of(
                        "reasonCode", saved.getReasonCode().name(),
                        "acknowledgedBy", actor != null ? actor.getDisplayName() : "OPERATOR",
                        "description", saved.getDescription() != null ? saved.getDescription() : ""
                )
        );

        return DowntimeEventDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<DowntimeEventDto> getPendingRootCauseEvents() {
        return downtimeEventRepository.findPendingRootCauses()
                .stream()
                .map(DowntimeEventDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MicroStopSummaryDto getMicroStopSummary(UUID machineId, Instant fromTime, Instant toTime) {
        Instant from = fromTime != null ? fromTime : Instant.now().minus(Duration.ofDays(1));
        Instant to = toTime != null ? toTime : Instant.now();

        Machine machine = null;
        String machineName = "All Machines";
        if (machineId != null) {
            machine = machineRepository.findByIdAndIsDeletedFalse(machineId).orElse(null);
            if (machine != null) {
                machineName = machine.getName();
            }
        }

        List<DowntimeEvent> events = downtimeEventRepository.findEventsInInterval(machineId, from, to);

        long microCount = 0;
        long microSeconds = 0;
        long majorCount = 0;
        long majorSeconds = 0;

        for (DowntimeEvent event : events) {
            Instant start = event.getStartTime();
            Instant end = event.getEndTime() != null ? event.getEndTime() : to;
            long dur = Math.max(0, Duration.between(start, end).toSeconds());

            if (event.isMicroStop() || (dur < MICRO_STOP_THRESHOLD_SECONDS && event.getEndTime() != null)) {
                microCount++;
                microSeconds += dur;
            } else {
                majorCount++;
                majorSeconds += dur;
            }
        }

        return new MicroStopSummaryDto(
                machineId,
                machineName,
                microCount,
                microSeconds,
                majorCount,
                majorSeconds
        );
    }
}
