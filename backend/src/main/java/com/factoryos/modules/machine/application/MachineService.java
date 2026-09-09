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
import com.factoryos.modules.tenant.context.TenantContextHolder;
import com.factoryos.modules.tenant.domain.Plant;
import com.factoryos.modules.tenant.domain.ProductionArea;
import com.factoryos.modules.tenant.domain.ProductionLine;
import com.factoryos.modules.tenant.domain.WorkCell;
import com.factoryos.modules.tenant.repository.PlantRepository;
import com.factoryos.modules.tenant.repository.ProductionAreaRepository;
import com.factoryos.modules.tenant.repository.ProductionLineRepository;
import com.factoryos.modules.tenant.repository.WorkCellRepository;
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
    private final PlantRepository plantRepository;
    private final ProductionAreaRepository areaRepository;
    private final ProductionLineRepository lineRepository;
    private final WorkCellRepository workCellRepository;

    public MachineService(
            MachineRepository machineRepository,
            AuditRecordingService auditRecordingService,
            PlantRepository plantRepository,
            ProductionAreaRepository areaRepository,
            ProductionLineRepository lineRepository,
            WorkCellRepository workCellRepository
    ) {
        this.machineRepository = machineRepository;
        this.auditRecordingService = auditRecordingService;
        this.plantRepository = plantRepository;
        this.areaRepository = areaRepository;
        this.lineRepository = lineRepository;
        this.workCellRepository = workCellRepository;
    }

    public PagedResponse<MachineDto> getMachines(MachineStatus status, String search, Pageable pageable) {
        UUID plantId = TenantContextHolder.getCurrentPlantId();
        Page<Machine> page;
        if (plantId != null && !TenantContextHolder.isGlobalAdmin()) {
            page = machineRepository.searchMachinesWithPlant(plantId, status, search, pageable);
        } else if (plantId != null) {
            page = machineRepository.searchMachinesWithPlant(plantId, status, search, pageable);
        } else {
            page = machineRepository.searchMachines(status, search, pageable);
        }
        return PagedResponse.from(page.map(MachineDto::from));
    }

    public MachineDto getMachineById(UUID id) {
        Machine machine = machineRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> AppException.notFound("Machine not found with ID: " + id));

        validatePlantAccess(machine);

        return MachineDto.from(machine);
    }

    @Transactional
    public MachineDto createMachine(CreateMachineRequest request, User actor) {
        String serial = request.getSerialNumber().trim().toUpperCase();
        if (machineRepository.existsBySerialNumberAndIsDeletedFalse(serial)) {
            throw AppException.conflict("DUPLICATE_SERIAL", "A machine with serial number '" + serial + "' already exists");
        }

        final UUID targetPlantId = request.getPlantId() != null ? request.getPlantId() :
                (TenantContextHolder.getCurrentPlantId() != null ? TenantContextHolder.getCurrentPlantId() : UUID.fromString("00000000-0000-0000-0000-000000000201"));

        Plant plant = plantRepository.findByIdAndIsDeletedFalse(targetPlantId)
                .orElseThrow(() -> AppException.notFound("Plant not found with ID: " + targetPlantId));

        Machine machine = new Machine();
        machine.setSerialNumber(serial);
        machine.setName(request.getName().trim());
        machine.setLocation(request.getLocation().trim());
        machine.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        machine.setStatus(request.getStatus() != null ? request.getStatus() : MachineStatus.IDLE);
        machine.setPlant(plant);

        if (request.getAreaId() != null) {
            ProductionArea area = areaRepository.findByIdAndIsDeletedFalse(request.getAreaId()).orElse(null);
            machine.setArea(area);
        }
        if (request.getLineId() != null) {
            ProductionLine line = lineRepository.findByIdAndIsDeletedFalse(request.getLineId()).orElse(null);
            machine.setLine(line);
        }
        if (request.getWorkCellId() != null) {
            WorkCell cell = workCellRepository.findByIdAndIsDeletedFalse(request.getWorkCellId()).orElse(null);
            machine.setWorkCell(cell);
        }

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
                        "status", saved.getStatus().name(),
                        "plantId", plant.getId().toString()
                )
        );

        return MachineDto.from(saved);
    }

    @Transactional
    public MachineDto updateMachine(UUID id, UpdateMachineRequest request, User actor) {
        Machine machine = machineRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> AppException.notFound("Machine not found with ID: " + id));

        validatePlantAccess(machine);

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
        if (request.getPlantId() != null) {
            Plant plant = plantRepository.findByIdAndIsDeletedFalse(request.getPlantId())
                    .orElseThrow(() -> AppException.notFound("Plant not found with ID: " + request.getPlantId()));
            machine.setPlant(plant);
        }
        if (request.getAreaId() != null) {
            ProductionArea area = areaRepository.findByIdAndIsDeletedFalse(request.getAreaId()).orElse(null);
            machine.setArea(area);
        }
        if (request.getLineId() != null) {
            ProductionLine line = lineRepository.findByIdAndIsDeletedFalse(request.getLineId()).orElse(null);
            machine.setLine(line);
        }
        if (request.getWorkCellId() != null) {
            WorkCell cell = workCellRepository.findByIdAndIsDeletedFalse(request.getWorkCellId()).orElse(null);
            machine.setWorkCell(cell);
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

        validatePlantAccess(machine);

        if (request.getExpectedVersion() == null || !request.getExpectedVersion().equals(machine.getVersion())) {
            throw AppException.versionConflict("Machine has been modified by another transaction. Reload and retry.");
        }

        MachineStatus previousStatus = machine.getStatus();
        machine.setStatus(request.getStatus());
        machine.setUpdatedBy(actor != null ? actor.getId() : null);
        machine.setUpdatedAt(Instant.now());

        Machine saved = machineRepository.save(machine);

        auditRecordingService.record(
                actor != null ? actor.getId() : null,
                "MACHINE_STATUS_UPDATED",
                "Machine",
                saved.getId(),
                Map.of("status", previousStatus.name()),
                Map.of("status", saved.getStatus().name())
        );

        return MachineDto.from(saved);
    }

    @Transactional
    public MachineDto archiveMachine(UUID id, Long expectedVersion, User actor) {
        Machine machine = machineRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> AppException.notFound("Machine not found with ID: " + id));

        validatePlantAccess(machine);

        if (expectedVersion == null || !expectedVersion.equals(machine.getVersion())) {
            throw AppException.versionConflict("Machine has been modified by another transaction. Reload and retry.");
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

    private void validatePlantAccess(Machine machine) {
        if (machine.getPlant() != null && !TenantContextHolder.isGlobalAdmin()) {
            UUID currentPlantId = TenantContextHolder.getCurrentPlantId();
            if (currentPlantId != null && !currentPlantId.equals(machine.getPlant().getId())) {
                throw AppException.forbidden("Cross-tenant access violation: Machine belongs to plant " + machine.getPlant().getCode());
            }
        }
    }
}
