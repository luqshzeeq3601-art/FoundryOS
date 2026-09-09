package com.factoryos.modules.tenant.application;

import com.factoryos.common.exception.AppException;
import com.factoryos.modules.audit.application.AuditRecordingService;
import com.factoryos.modules.auth.domain.Role;
import com.factoryos.modules.auth.domain.User;
import com.factoryos.modules.auth.repository.RoleRepository;
import com.factoryos.modules.auth.repository.UserRepository;
import com.factoryos.modules.machine.domain.Machine;
import com.factoryos.modules.machine.dto.MachineDto;
import com.factoryos.modules.machine.repository.MachineRepository;
import com.factoryos.modules.tenant.domain.*;
import com.factoryos.modules.tenant.dto.*;
import com.factoryos.modules.tenant.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class HierarchyService {

    private final EnterpriseRepository enterpriseRepository;
    private final PlantRepository plantRepository;
    private final ProductionAreaRepository areaRepository;
    private final ProductionLineRepository lineRepository;
    private final WorkCellRepository workCellRepository;
    private final UserPlantMembershipRepository membershipRepository;
    private final MachineRepository machineRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuditRecordingService auditRecordingService;

    public HierarchyService(
            EnterpriseRepository enterpriseRepository,
            PlantRepository plantRepository,
            ProductionAreaRepository areaRepository,
            ProductionLineRepository lineRepository,
            WorkCellRepository workCellRepository,
            UserPlantMembershipRepository membershipRepository,
            MachineRepository machineRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            AuditRecordingService auditRecordingService
    ) {
        this.enterpriseRepository = enterpriseRepository;
        this.plantRepository = plantRepository;
        this.areaRepository = areaRepository;
        this.lineRepository = lineRepository;
        this.workCellRepository = workCellRepository;
        this.membershipRepository = membershipRepository;
        this.machineRepository = machineRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.auditRecordingService = auditRecordingService;
    }

    public List<EnterpriseDto> getEnterprises() {
        return enterpriseRepository.findAllByIsDeletedFalseOrderByCodeAsc().stream()
                .map(EnterpriseDto::from)
                .toList();
    }

    public List<PlantDto> getAuthorizedPlants(User user) {
        if (user == null) {
            return List.of();
        }

        boolean isGlobalAdmin = user.getRole() != null && "ADMIN".equalsIgnoreCase(user.getRole().getName().name());
        if (isGlobalAdmin) {
            return plantRepository.findAllByIsDeletedFalseOrderByCodeAsc().stream()
                    .map(PlantDto::from)
                    .toList();
        }

        List<UserPlantMembership> memberships = membershipRepository.findByUserId(user.getId());
        return memberships.stream()
                .map(UserPlantMembership::getPlant)
                .filter(p -> !p.isDeleted())
                .map(PlantDto::from)
                .toList();
    }

    public PlantDto getPlantById(UUID plantId) {
        Plant plant = plantRepository.findByIdAndIsDeletedFalse(plantId)
                .orElseThrow(() -> AppException.notFound("Plant not found with ID: " + plantId));
        return PlantDto.from(plant);
    }

    @Transactional(readOnly = true)
    public HierarchyTreeDto getPlantHierarchyTree(UUID plantId) {
        Plant plant = plantRepository.findByIdAndIsDeletedFalse(plantId)
                .orElseThrow(() -> AppException.notFound("Plant not found with ID: " + plantId));

        List<ProductionArea> areas = areaRepository.findByPlantIdAndIsDeletedFalseOrderByCodeAsc(plantId);
        List<HierarchyTreeDto.AreaNodeDto> areaNodes = new ArrayList<>();

        for (ProductionArea area : areas) {
            List<ProductionLine> lines = lineRepository.findByAreaIdAndIsDeletedFalseOrderByCodeAsc(area.getId());
            List<HierarchyTreeDto.LineNodeDto> lineNodes = new ArrayList<>();

            for (ProductionLine line : lines) {
                List<WorkCell> cells = workCellRepository.findByLineIdAndIsDeletedFalseOrderByCodeAsc(line.getId());
                List<HierarchyTreeDto.WorkCellNodeDto> cellNodes = new ArrayList<>();

                for (WorkCell cell : cells) {
                    List<Machine> machines = machineRepository.findByWorkCellIdAndIsDeletedFalse(cell.getId());
                    List<MachineDto> machineDtos = machines.stream()
                            .map(MachineDto::from)
                            .toList();

                    cellNodes.add(new HierarchyTreeDto.WorkCellNodeDto(
                            cell.getId(),
                            cell.getCode(),
                            cell.getName(),
                            machineDtos
                    ));
                }

                lineNodes.add(new HierarchyTreeDto.LineNodeDto(
                        line.getId(),
                        line.getCode(),
                        line.getName(),
                        cellNodes
                ));
            }

            areaNodes.add(new HierarchyTreeDto.AreaNodeDto(
                    area.getId(),
                    area.getCode(),
                    area.getName(),
                    lineNodes
            ));
        }

        return new HierarchyTreeDto(
                plant.getId(),
                plant.getCode(),
                plant.getName(),
                plant.getTimezone(),
                plant.getStatus(),
                areaNodes
        );
    }

    @Transactional
    public PlantDto createPlant(CreatePlantRequest request, User actor) {
        Enterprise enterprise = enterpriseRepository.findByIdAndIsDeletedFalse(request.enterpriseId())
                .orElseThrow(() -> AppException.notFound("Enterprise not found with ID: " + request.enterpriseId()));

        String code = request.code().trim().toUpperCase();
        if (plantRepository.existsByCodeIgnoreCaseAndIsDeletedFalse(code)) {
            throw AppException.conflict("DUPLICATE_PLANT_CODE", "Plant with code '" + code + "' already exists");
        }

        Plant plant = new Plant();
        plant.setEnterprise(enterprise);
        plant.setCode(code);
        plant.setName(request.name().trim());
        plant.setTimezone(request.timezone() != null && !request.timezone().isBlank() ? request.timezone().trim() : "UTC");
        plant.setAddress(request.address() != null ? request.address().trim() : null);
        plant.setStatus("ACTIVE");
        plant.setCreatedBy(actor != null ? actor.getId() : null);
        plant.setUpdatedBy(actor != null ? actor.getId() : null);

        Plant saved = plantRepository.save(plant);

        auditRecordingService.record(
                actor != null ? actor.getId() : null,
                "PLANT_CREATED",
                "Plant",
                saved.getId(),
                null,
                Map.of("code", saved.getCode(), "name", saved.getName(), "enterpriseId", enterprise.getId().toString())
        );

        return PlantDto.from(saved);
    }

    @Transactional
    public ProductionAreaDto createArea(CreateAreaRequest request, User actor) {
        Plant plant = plantRepository.findByIdAndIsDeletedFalse(request.plantId())
                .orElseThrow(() -> AppException.notFound("Plant not found with ID: " + request.plantId()));

        String code = request.code().trim().toUpperCase();
        if (areaRepository.existsByPlantIdAndCodeIgnoreCaseAndIsDeletedFalse(plant.getId(), code)) {
            throw AppException.conflict("DUPLICATE_AREA_CODE", "Area with code '" + code + "' already exists at plant " + plant.getCode());
        }

        ProductionArea area = new ProductionArea();
        area.setPlant(plant);
        area.setCode(code);
        area.setName(request.name().trim());
        area.setDescription(request.description() != null ? request.description().trim() : null);
        area.setCreatedBy(actor != null ? actor.getId() : null);
        area.setUpdatedBy(actor != null ? actor.getId() : null);

        ProductionArea saved = areaRepository.save(area);

        auditRecordingService.record(
                actor != null ? actor.getId() : null,
                "PRODUCTION_AREA_CREATED",
                "ProductionArea",
                saved.getId(),
                null,
                Map.of("code", saved.getCode(), "name", saved.getName(), "plantId", plant.getId().toString())
        );

        return ProductionAreaDto.from(saved);
    }

    @Transactional
    public ProductionLineDto createLine(CreateLineRequest request, User actor) {
        ProductionArea area = areaRepository.findByIdAndIsDeletedFalse(request.areaId())
                .orElseThrow(() -> AppException.notFound("Area not found with ID: " + request.areaId()));

        String code = request.code().trim().toUpperCase();
        if (lineRepository.existsByAreaIdAndCodeIgnoreCaseAndIsDeletedFalse(area.getId(), code)) {
            throw AppException.conflict("DUPLICATE_LINE_CODE", "Line with code '" + code + "' already exists in area " + area.getCode());
        }

        ProductionLine line = new ProductionLine();
        line.setArea(area);
        line.setCode(code);
        line.setName(request.name().trim());
        line.setDescription(request.description() != null ? request.description().trim() : null);
        line.setCreatedBy(actor != null ? actor.getId() : null);
        line.setUpdatedBy(actor != null ? actor.getId() : null);

        ProductionLine saved = lineRepository.save(line);

        auditRecordingService.record(
                actor != null ? actor.getId() : null,
                "PRODUCTION_LINE_CREATED",
                "ProductionLine",
                saved.getId(),
                null,
                Map.of("code", saved.getCode(), "name", saved.getName(), "areaId", area.getId().toString())
        );

        return ProductionLineDto.from(saved);
    }

    @Transactional
    public WorkCellDto createWorkCell(CreateWorkCellRequest request, User actor) {
        ProductionLine line = lineRepository.findByIdAndIsDeletedFalse(request.lineId())
                .orElseThrow(() -> AppException.notFound("Line not found with ID: " + request.lineId()));

        String code = request.code().trim().toUpperCase();
        if (workCellRepository.existsByLineIdAndCodeIgnoreCaseAndIsDeletedFalse(line.getId(), code)) {
            throw AppException.conflict("DUPLICATE_CELL_CODE", "Work cell with code '" + code + "' already exists on line " + line.getCode());
        }

        WorkCell cell = new WorkCell();
        cell.setLine(line);
        cell.setCode(code);
        cell.setName(request.name().trim());
        cell.setDescription(request.description() != null ? request.description().trim() : null);
        cell.setCreatedBy(actor != null ? actor.getId() : null);
        cell.setUpdatedBy(actor != null ? actor.getId() : null);

        WorkCell saved = workCellRepository.save(cell);

        auditRecordingService.record(
                actor != null ? actor.getId() : null,
                "WORK_CELL_CREATED",
                "WorkCell",
                saved.getId(),
                null,
                Map.of("code", saved.getCode(), "name", saved.getName(), "lineId", line.getId().toString())
        );

        return WorkCellDto.from(saved);
    }

    public List<UserPlantMembershipDto> getUserMemberships(UUID userId) {
        return membershipRepository.findByUserId(userId).stream()
                .map(UserPlantMembershipDto::from)
                .toList();
    }

    @Transactional
    public UserPlantMembershipDto assignPlantMembership(UUID userId, AssignPlantMembershipRequest request, User actor) {
        User targetUser = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> AppException.notFound("User not found with ID: " + userId));

        Plant plant = plantRepository.findByIdAndIsDeletedFalse(request.plantId())
                .orElseThrow(() -> AppException.notFound("Plant not found with ID: " + request.plantId()));

        Role role = roleRepository.findById(request.roleId())
                .orElseThrow(() -> AppException.notFound("Role not found with ID: " + request.roleId()));

        Optional<UserPlantMembership> existing = membershipRepository.findByUserIdAndPlantId(userId, plant.getId());
        UserPlantMembership membership;

        if (existing.isPresent()) {
            membership = existing.get();
            membership.setRole(role);
            membership.setDefault(request.isDefault());
        } else {
            membership = new UserPlantMembership();
            membership.setUser(targetUser);
            membership.setPlant(plant);
            membership.setRole(role);
            membership.setDefault(request.isDefault());
        }

        UserPlantMembership saved = membershipRepository.save(membership);

        auditRecordingService.record(
                actor != null ? actor.getId() : null,
                "PLANT_MEMBERSHIP_ASSIGNED",
                "UserPlantMembership",
                saved.getId(),
                null,
                Map.of("userId", userId.toString(), "plantId", plant.getId().toString(), "role", role.getName().name())
        );

        return UserPlantMembershipDto.from(saved);
    }

    @Transactional
    public void removePlantMembership(UUID userId, UUID plantId, User actor) {
        membershipRepository.deleteByUserIdAndPlantId(userId, plantId);
        auditRecordingService.record(
                actor != null ? actor.getId() : null,
                "PLANT_MEMBERSHIP_REMOVED",
                "UserPlantMembership",
                userId,
                null,
                Map.of("userId", userId.toString(), "plantId", plantId.toString())
        );
    }
}
