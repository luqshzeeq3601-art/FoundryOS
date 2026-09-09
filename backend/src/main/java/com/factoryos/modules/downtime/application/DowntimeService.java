package com.factoryos.modules.downtime.application;

import com.factoryos.common.dto.PagedResponse;
import com.factoryos.common.exception.AppException;
import com.factoryos.modules.audit.application.AuditRecordingService;
import com.factoryos.modules.auth.domain.User;
import com.factoryos.modules.downtime.domain.DowntimeEvent;
import com.factoryos.modules.downtime.dto.CreateDowntimeRequest;
import com.factoryos.modules.downtime.dto.DowntimeEventDto;
import com.factoryos.modules.downtime.dto.ResolveDowntimeRequest;
import com.factoryos.modules.downtime.repository.DowntimeEventRepository;
import com.factoryos.modules.machine.domain.Machine;
import com.factoryos.modules.machine.domain.MachineStatus;
import com.factoryos.modules.machine.repository.MachineRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class DowntimeService {

    private final DowntimeEventRepository downtimeEventRepository;
    private final MachineRepository machineRepository;
    private final AuditRecordingService auditRecordingService;

    public DowntimeService(
            DowntimeEventRepository downtimeEventRepository,
            MachineRepository machineRepository,
            AuditRecordingService auditRecordingService
    ) {
        this.downtimeEventRepository = downtimeEventRepository;
        this.machineRepository = machineRepository;
        this.auditRecordingService = auditRecordingService;
    }

    public PagedResponse<DowntimeEventDto> getDowntimeEvents(
            UUID machineId,
            Boolean openOnly,
            Instant fromTime,
            Instant toTime,
            Pageable pageable
    ) {
        Page<DowntimeEvent> page = downtimeEventRepository.searchDowntimeEvents(machineId, openOnly, fromTime, toTime, pageable);
        return PagedResponse.from(page.map(DowntimeEventDto::from));
    }

    public DowntimeEventDto getDowntimeEventById(UUID id) {
        DowntimeEvent event = downtimeEventRepository.findById(id)
                .filter(e -> !e.isDeleted())
                .orElseThrow(() -> AppException.notFound("Downtime event not found with ID: " + id));
        return DowntimeEventDto.from(event);
    }

    public Optional<DowntimeEventDto> getActiveDowntimeForMachine(UUID machineId) {
        return downtimeEventRepository.findByMachineIdAndEndTimeIsNull(machineId)
                .filter(e -> !e.isDeleted())
                .map(DowntimeEventDto::from);
    }

    @Transactional
    public DowntimeEventDto createDowntimeEvent(CreateDowntimeRequest request, User actor) {
        Machine machine = machineRepository.findByIdAndIsDeletedFalse(request.getMachineId())
                .orElseThrow(() -> AppException.notFound("Machine not found with ID: " + request.getMachineId()));

        Optional<DowntimeEvent> openEvent = downtimeEventRepository.findByMachineAndEndTimeIsNull(machine);
        if (openEvent.isPresent()) {
            throw AppException.conflict("OPEN_DOWNTIME_EXISTS", "Machine " + machine.getName() + " already has an open downtime event.");
        }

        Instant startTime = request.getStartTime() != null ? request.getStartTime() : Instant.now();

        DowntimeEvent event = new DowntimeEvent();
        event.setMachine(machine);
        event.setReasonCode(request.getReasonCode());
        event.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        event.setStartTime(startTime);
        event.setCreatedBy(actor != null ? actor.getId() : null);
        event.setUpdatedBy(actor != null ? actor.getId() : null);

        DowntimeEvent saved = downtimeEventRepository.save(event);

        // Update machine status to DOWN
        if (machine.getStatus() != MachineStatus.DOWN) {
            machine.setStatus(MachineStatus.DOWN);
            machine.setUpdatedBy(actor != null ? actor.getId() : null);
            machine.setUpdatedAt(Instant.now());
            machineRepository.save(machine);
        }

        auditRecordingService.record(
                actor != null ? actor.getId() : null,
                "DOWNTIME_STARTED",
                "DowntimeEvent",
                saved.getId(),
                null,
                Map.of(
                        "machineId", machine.getId().toString(),
                        "machineName", machine.getName(),
                        "reasonCode", saved.getReasonCode().name(),
                        "startTime", saved.getStartTime().toString()
                )
        );

        return DowntimeEventDto.from(saved);
    }

    @Transactional
    public DowntimeEventDto resolveDowntimeEvent(UUID id, ResolveDowntimeRequest request, User actor) {
        DowntimeEvent event = downtimeEventRepository.findById(id)
                .filter(e -> !e.isDeleted())
                .orElseThrow(() -> AppException.notFound("Downtime event not found with ID: " + id));

        if (event.getEndTime() != null) {
            throw AppException.conflict("ALREADY_RESOLVED", "Downtime event has already been resolved.");
        }

        if (request.getExpectedVersion() == null || !request.getExpectedVersion().equals(event.getVersion())) {
            throw AppException.versionConflict("Downtime event modified by another user. Reload and retry.");
        }

        Instant endTime = request.getEndTime() != null ? request.getEndTime() : Instant.now();
        if (endTime.isBefore(event.getStartTime())) {
            throw AppException.badRequest("End time cannot be earlier than start time.");
        }

        event.setEndTime(endTime);
        event.setResolutionNote(request.getResolutionNote().trim());
        event.setResolvedBy(actor);
        event.setUpdatedBy(actor != null ? actor.getId() : null);
        event.setUpdatedAt(Instant.now());

        DowntimeEvent saved = downtimeEventRepository.save(event);

        // Transition machine back to IDLE if it is DOWN
        Machine machine = saved.getMachine();
        if (machine.getStatus() == MachineStatus.DOWN) {
            machine.setStatus(MachineStatus.IDLE);
            machine.setUpdatedBy(actor != null ? actor.getId() : null);
            machine.setUpdatedAt(Instant.now());
            machineRepository.save(machine);
        }

        auditRecordingService.record(
                actor != null ? actor.getId() : null,
                "DOWNTIME_RESOLVED",
                "DowntimeEvent",
                saved.getId(),
                Map.of("status", "OPEN"),
                Map.of(
                        "endTime", saved.getEndTime().toString(),
                        "resolutionNote", saved.getResolutionNote(),
                        "resolvedBy", actor != null ? actor.getDisplayName() : "SYSTEM"
                )
        );

        return DowntimeEventDto.from(saved);
    }
}
