package com.factoryos.modules.machine.application;

import com.factoryos.common.dto.PagedResponse;
import com.factoryos.common.exception.AppException;
import com.factoryos.modules.audit.application.AuditRecordingService;
import com.factoryos.modules.auth.domain.User;
import com.factoryos.modules.machine.domain.Machine;
import com.factoryos.modules.machine.domain.MachineStatus;
import com.factoryos.modules.machine.dto.CreateMachineRequest;
import com.factoryos.modules.machine.dto.MachineDto;
import com.factoryos.modules.machine.dto.UpdateMachineRequest;
import com.factoryos.modules.machine.dto.UpdateMachineStatusRequest;
import com.factoryos.modules.machine.repository.MachineRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class MachineService {

    private final MachineRepository machineRepository;
    private final AuditRecordingService auditRecordingService;

    public MachineService(MachineRepository machineRepository, AuditRecordingService auditRecordingService) {
        this.machineRepository = machineRepository;
        this.auditRecordingService = auditRecordingService;
    }

    public PagedResponse<MachineDto> getMachines(MachineStatus status, String search, Pageable pageable) {
        Page<Machine> page = machineRepository.searchMachines(status, search, pageable);
        return PagedResponse.from(page.map(MachineDto::from));
    }

    public MachineDto getMachineById(UUID id) {
        Machine machine = machineRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> AppException.notFound("Machine not found with ID: " + id));
        return MachineDto.from(machine);
    }

    @Transactional
    public MachineDto createMachine(CreateMachineRequest request, User actor) {
        String serial = request.getSerialNumber().trim().toUpperCase();
        if (machineRepository.existsBySerialNumberAndIsDeletedFalse(serial)) {
            throw AppException.conflict("DUPLICATE_SERIAL", "A machine with serial number '" + serial + "' already exists");
        }

        Machine machine = new Machine();
        machine.setSerialNumber(serial);
        machine.setName(request.getName().trim());
        machine.setLocation(request.getLocation().trim());
        machine.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        machine.setStatus(request.getStatus() != null ? request.getStatus() : MachineStatus.IDLE);
        machine.setCreatedBy(actor != null ? actor.getId() : null);
        machine.setUpdatedBy(actor != null ? actor.getId() : null);

        Machine saved = machineRepository.save(machine);

        auditRecordingService.record(
                actor != null ? actor.getId() : null,
                "MACHINE_CREATED",
                "Machine",
                saved.getId(),
                null,
                Map.of(
                        "serialNumber", saved.getSerialNumber(),
                        "name", saved.getName(),
                        "location", saved.getLocation(),
                        "status", saved.getStatus().name()
                )
        );

        return MachineDto.from(saved);
    }

    @Transactional
    public MachineDto updateMachine(UUID id, UpdateMachineRequest request, User actor) {
        Machine machine = machineRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> AppException.notFound("Machine not found with ID: " + id));

        if (request.getExpectedVersion() == null || !request.getExpectedVersion().equals(machine.getVersion())) {
            throw AppException.versionConflict("Machine has been modified by another transaction. Reload and retry.");
        }

        Map<String, Object> beforeState = Map.of(
                "name", machine.getName(),
                "location", machine.getLocation(),
                "status", machine.getStatus().name()
        );

        if (request.getName() != null && !request.getName().isBlank()) {
            machine.setName(request.getName().trim());
        }
        if (request.getLocation() != null && !request.getLocation().isBlank()) {
            machine.setLocation(request.getLocation().trim());
        }
        if (request.getDescription() != null) {
            machine.setDescription(request.getDescription().trim());
        }

        machine.setUpdatedBy(actor != null ? actor.getId() : null);
        machine.setUpdatedAt(Instant.now());

        Machine saved = machineRepository.save(machine);

        auditRecordingService.record(
                actor != null ? actor.getId() : null,
                "MACHINE_UPDATED",
                "Machine",
                saved.getId(),
                beforeState,
                Map.of(
                        "name", saved.getName(),
                        "location", saved.getLocation(),
                        "status", saved.getStatus().name()
                )
        );

        return MachineDto.from(saved);
    }

    @Transactional
    public MachineDto updateMachineStatus(UUID id, UpdateMachineStatusRequest request, User actor) {
        Machine machine = machineRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> AppException.notFound("Machine not found with ID: " + id));

        if (request.getExpectedVersion() == null || !request.getExpectedVersion().equals(machine.getVersion())) {
            throw AppException.versionConflict("Machine status changed by another operator. Reload and retry.");
        }

        MachineStatus oldStatus = machine.getStatus();
        machine.setStatus(request.getStatus());
        machine.setUpdatedBy(actor != null ? actor.getId() : null);
        machine.setUpdatedAt(Instant.now());

        Machine saved = machineRepository.save(machine);

        auditRecordingService.record(
                actor != null ? actor.getId() : null,
                "MACHINE_STATUS_CHANGED",
                "Machine",
                saved.getId(),
                Map.of("status", oldStatus.name()),
                Map.of("status", saved.getStatus().name())
        );

        return MachineDto.from(saved);
    }

    @Transactional
    public MachineDto archiveMachine(UUID id, Long expectedVersion, User actor) {
        Machine machine = machineRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> AppException.notFound("Machine not found with ID: " + id));

        if (expectedVersion == null || !expectedVersion.equals(machine.getVersion())) {
            throw AppException.versionConflict("Machine modified by another transaction. Reload and retry.");
        }

        machine.setDeleted(true);
        machine.setUpdatedBy(actor != null ? actor.getId() : null);
        machine.setUpdatedAt(Instant.now());

        Machine saved = machineRepository.save(machine);

        auditRecordingService.record(
                actor != null ? actor.getId() : null,
                "MACHINE_ARCHIVED",
                "Machine",
                saved.getId(),
                Map.of("isDeleted", false),
                Map.of("isDeleted", true)
        );

        return MachineDto.from(saved);
    }
}
