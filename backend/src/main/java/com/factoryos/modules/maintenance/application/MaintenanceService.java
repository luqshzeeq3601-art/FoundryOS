package com.factoryos.modules.maintenance.application;

import com.factoryos.common.dto.PagedResponse;
import com.factoryos.common.exception.AppException;
import com.factoryos.modules.audit.application.AuditRecordingService;
import com.factoryos.modules.auth.domain.RoleType;
import com.factoryos.modules.auth.domain.User;
import com.factoryos.modules.auth.repository.UserRepository;
import com.factoryos.modules.downtime.domain.DowntimeEvent;
import com.factoryos.modules.downtime.repository.DowntimeEventRepository;
import com.factoryos.modules.machine.domain.Machine;
import com.factoryos.modules.machine.repository.MachineRepository;
import com.factoryos.modules.maintenance.domain.MaintenancePriority;
import com.factoryos.modules.maintenance.domain.MaintenanceStatus;
import com.factoryos.modules.maintenance.domain.MaintenanceWorkOrder;
import com.factoryos.modules.maintenance.dto.*;
import com.factoryos.modules.maintenance.repository.MaintenanceWorkOrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class MaintenanceService {

    private final MaintenanceWorkOrderRepository maintenanceWorkOrderRepository;
    private final MachineRepository machineRepository;
    private final DowntimeEventRepository downtimeEventRepository;
    private final UserRepository userRepository;
    private final AuditRecordingService auditRecordingService;

    public MaintenanceService(
            MaintenanceWorkOrderRepository maintenanceWorkOrderRepository,
            MachineRepository machineRepository,
            DowntimeEventRepository downtimeEventRepository,
            UserRepository userRepository,
            AuditRecordingService auditRecordingService
    ) {
        this.maintenanceWorkOrderRepository = maintenanceWorkOrderRepository;
        this.machineRepository = machineRepository;
        this.downtimeEventRepository = downtimeEventRepository;
        this.userRepository = userRepository;
        this.auditRecordingService = auditRecordingService;
    }

    public PagedResponse<WorkOrderDto> getWorkOrders(
            UUID machineId,
            MaintenancePriority priority,
            MaintenanceStatus status,
            Boolean isPrescriptive,
            UUID assignedToId,
            String search,
            Pageable pageable
    ) {
        Page<MaintenanceWorkOrder> page = maintenanceWorkOrderRepository.searchWorkOrders(
                machineId, priority, status, isPrescriptive, assignedToId, search, pageable
        );
        return PagedResponse.from(page.map(WorkOrderDto::from));
    }

    public WorkOrderDto getWorkOrderById(UUID id) {
        MaintenanceWorkOrder order = maintenanceWorkOrderRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> AppException.notFound("Maintenance work order not found with ID: " + id));
        return WorkOrderDto.from(order);
    }

    public boolean hasActiveWorkOrdersForUser(User user) {
        long count = maintenanceWorkOrderRepository.countByAssignedToAndStatusIn(
                user, List.of(MaintenanceStatus.ASSIGNED, MaintenanceStatus.IN_PROGRESS)
        );
        return count > 0;
    }

    @Transactional
    public WorkOrderDto createWorkOrder(CreateWorkOrderRequest request, User actor) {
        String woNumber = request.getWorkOrderNumber().trim().toUpperCase();
        if (maintenanceWorkOrderRepository.existsByWorkOrderNumberAndIsDeletedFalse(woNumber)) {
            throw AppException.conflict("DUPLICATE_WORK_ORDER", "Work order number '" + woNumber + "' already exists");
        }

        Machine machine = machineRepository.findByIdAndIsDeletedFalse(request.getMachineId())
                .orElseThrow(() -> AppException.notFound("Machine not found with ID: " + request.getMachineId()));

        DowntimeEvent downtimeEvent = null;
        if (request.getDowntimeEventId() != null) {
            downtimeEvent = downtimeEventRepository.findById(request.getDowntimeEventId())
                    .filter(d -> !d.isDeleted())
                    .orElseThrow(() -> AppException.notFound("Downtime event not found with ID: " + request.getDowntimeEventId()));

            if (!downtimeEvent.getMachine().getId().equals(machine.getId())) {
                throw AppException.badRequest("Downtime event does not belong to machine " + machine.getName());
            }
        }

        User assignedTo = null;
        MaintenanceStatus initialStatus = MaintenanceStatus.OPEN;
        if (request.getAssignedTo() != null) {
            assignedTo = userRepository.findByIdAndIsDeletedFalse(request.getAssignedTo())
                    .orElseThrow(() -> AppException.notFound("Assigned technician not found with ID: " + request.getAssignedTo()));
            if (!assignedTo.isActive()) {
                throw AppException.badRequest("Cannot assign inactive technician.");
            }
            initialStatus = MaintenanceStatus.ASSIGNED;
        }

        MaintenanceWorkOrder order = new MaintenanceWorkOrder();
        order.setWorkOrderNumber(woNumber);
        order.setMachine(machine);
        order.setDowntimeEvent(downtimeEvent);
        order.setTitle(request.getTitle().trim());
        order.setDescription(request.getDescription().trim());
        order.setPriority(request.getPriority() != null ? request.getPriority() : MaintenancePriority.MEDIUM);
        order.setStatus(initialStatus);
        order.setAssignedTo(assignedTo);
        order.setDueAt(request.getDueAt());
        order.setCreatedBy(actor != null ? actor.getId() : null);
        order.setUpdatedBy(actor != null ? actor.getId() : null);

        MaintenanceWorkOrder saved = maintenanceWorkOrderRepository.save(order);

        auditRecordingService.record(
                actor != null ? actor.getId() : null,
                "WORK_ORDER_CREATED",
                "MaintenanceWorkOrder",
                saved.getId(),
                null,
                Map.of(
                        "workOrderNumber", saved.getWorkOrderNumber(),
                        "machineId", machine.getId().toString(),
                        "priority", saved.getPriority().name(),
                        "status", saved.getStatus().name()
                )
        );

        return WorkOrderDto.from(saved);
    }

    @Transactional
    public WorkOrderDto updateWorkOrder(UUID id, UpdateWorkOrderRequest request, User actor) {
        MaintenanceWorkOrder order = maintenanceWorkOrderRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> AppException.notFound("Maintenance work order not found with ID: " + id));

        if (request.getExpectedVersion() == null || !request.getExpectedVersion().equals(order.getVersion())) {
            throw AppException.versionConflict("Work order modified by another user. Reload and retry.");
        }

        if (order.getStatus() == MaintenanceStatus.COMPLETED || order.getStatus() == MaintenanceStatus.CANCELLED) {
            throw AppException.badRequest("Cannot edit completed or cancelled work orders.");
        }

        Map<String, Object> beforeState = Map.of(
                "title", order.getTitle(),
                "priority", order.getPriority().name()
        );

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            order.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            order.setDescription(request.getDescription().trim());
        }
        if (request.getPriority() != null) {
            order.setPriority(request.getPriority());
        }
        if (request.getDueAt() != null) {
            order.setDueAt(request.getDueAt());
        }

        order.setUpdatedBy(actor != null ? actor.getId() : null);
        order.setUpdatedAt(Instant.now());

        MaintenanceWorkOrder saved = maintenanceWorkOrderRepository.save(order);

        auditRecordingService.record(
                actor != null ? actor.getId() : null,
                "WORK_ORDER_UPDATED",
                "MaintenanceWorkOrder",
                saved.getId(),
                beforeState,
                Map.of("title", saved.getTitle(), "priority", saved.getPriority().name())
        );

        return WorkOrderDto.from(saved);
    }

    @Transactional
    public WorkOrderDto assignWorkOrder(UUID id, AssignWorkOrderRequest request, User actor) {
        MaintenanceWorkOrder order = maintenanceWorkOrderRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> AppException.notFound("Maintenance work order not found with ID: " + id));

        if (request.getExpectedVersion() == null || !request.getExpectedVersion().equals(order.getVersion())) {
            throw AppException.versionConflict("Work order assignment changed by another user. Reload and retry.");
        }

        if (order.getStatus() == MaintenanceStatus.COMPLETED || order.getStatus() == MaintenanceStatus.CANCELLED) {
            throw AppException.badRequest("Cannot assign completed or cancelled work order.");
        }

        User technician = userRepository.findByIdAndIsDeletedFalse(request.getAssignedTo())
                .orElseThrow(() -> AppException.notFound("Technician not found with ID: " + request.getAssignedTo()));

        if (!technician.isActive()) {
            throw AppException.badRequest("Cannot assign work order to inactive technician.");
        }

        order.setAssignedTo(technician);
        if (order.getStatus() == MaintenanceStatus.OPEN) {
            order.setStatus(MaintenanceStatus.ASSIGNED);
        }
        order.setUpdatedBy(actor != null ? actor.getId() : null);
        order.setUpdatedAt(Instant.now());

        MaintenanceWorkOrder saved = maintenanceWorkOrderRepository.save(order);

        auditRecordingService.record(
                actor != null ? actor.getId() : null,
                "WORK_ORDER_ASSIGNED",
                "MaintenanceWorkOrder",
                saved.getId(),
                null,
                Map.of("assignedTo", technician.getDisplayName(), "status", saved.getStatus().name())
        );

        return WorkOrderDto.from(saved);
    }

    @Transactional
    public WorkOrderDto startWorkOrder(UUID id, Long expectedVersion, User actor) {
        MaintenanceWorkOrder order = maintenanceWorkOrderRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> AppException.notFound("Maintenance work order not found with ID: " + id));

        if (expectedVersion == null || !expectedVersion.equals(order.getVersion())) {
            throw AppException.versionConflict("Work order status changed by another user. Reload and retry.");
        }

        if (order.getStatus() != MaintenanceStatus.ASSIGNED && order.getStatus() != MaintenanceStatus.OPEN) {
            throw AppException.badRequest("Can only start work orders in OPEN or ASSIGNED status.");
        }

        if (order.getAssignedTo() == null) {
            order.setAssignedTo(actor);
        }

        order.setStatus(MaintenanceStatus.IN_PROGRESS);
        order.setStartedAt(Instant.now());
        order.setUpdatedBy(actor != null ? actor.getId() : null);
        order.setUpdatedAt(Instant.now());

        MaintenanceWorkOrder saved = maintenanceWorkOrderRepository.save(order);

        auditRecordingService.record(
                actor != null ? actor.getId() : null,
                "WORK_ORDER_STARTED",
                "MaintenanceWorkOrder",
                saved.getId(),
                Map.of("status", "ASSIGNED"),
                Map.of("status", "IN_PROGRESS", "startedAt", saved.getStartedAt().toString())
        );

        return WorkOrderDto.from(saved);
    }

    @Transactional
    public WorkOrderDto completeWorkOrder(UUID id, CompleteWorkOrderRequest request, User actor) {
        MaintenanceWorkOrder order = maintenanceWorkOrderRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> AppException.notFound("Maintenance work order not found with ID: " + id));

        if (request.getExpectedVersion() == null || !request.getExpectedVersion().equals(order.getVersion())) {
            throw AppException.versionConflict("Work order status changed by another user. Reload and retry.");
        }

        if (order.getStatus() != MaintenanceStatus.IN_PROGRESS && order.getStatus() != MaintenanceStatus.ASSIGNED) {
            throw AppException.badRequest("Can only complete work orders in ASSIGNED or IN_PROGRESS status.");
        }

        order.setStatus(MaintenanceStatus.COMPLETED);
        order.setCompletionNote(request.getCompletionNote().trim());
        order.setCompletedAt(Instant.now());
        order.setClosedAt(Instant.now());
        order.setUpdatedBy(actor != null ? actor.getId() : null);
        order.setUpdatedAt(Instant.now());

        MaintenanceWorkOrder saved = maintenanceWorkOrderRepository.save(order);

        auditRecordingService.record(
                actor != null ? actor.getId() : null,
                "WORK_ORDER_COMPLETED",
                "MaintenanceWorkOrder",
                saved.getId(),
                Map.of("status", "IN_PROGRESS"),
                Map.of(
                        "status", "COMPLETED",
                        "completedAt", saved.getCompletedAt().toString(),
                        "completionNote", saved.getCompletionNote()
                )
        );

        return WorkOrderDto.from(saved);
    }

    @Transactional
    public WorkOrderDto cancelWorkOrder(UUID id, CancelWorkOrderRequest request, User actor) {
        MaintenanceWorkOrder order = maintenanceWorkOrderRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> AppException.notFound("Maintenance work order not found with ID: " + id));

        if (request.getExpectedVersion() == null || !request.getExpectedVersion().equals(order.getVersion())) {
            throw AppException.versionConflict("Work order status changed by another user. Reload and retry.");
        }

        if (order.getStatus() == MaintenanceStatus.COMPLETED || order.getStatus() == MaintenanceStatus.CANCELLED) {
            throw AppException.badRequest("Cannot cancel already completed or cancelled work orders.");
        }

        MaintenanceStatus prevStatus = order.getStatus();
        order.setStatus(MaintenanceStatus.CANCELLED);
        order.setCancellationNote(request.getCancellationNote().trim());
        order.setClosedAt(Instant.now());
        order.setUpdatedBy(actor != null ? actor.getId() : null);
        order.setUpdatedAt(Instant.now());

        MaintenanceWorkOrder saved = maintenanceWorkOrderRepository.save(order);

        auditRecordingService.record(
                actor != null ? actor.getId() : null,
                "WORK_ORDER_CANCELLED",
                "MaintenanceWorkOrder",
                saved.getId(),
                Map.of("status", prevStatus.name()),
                Map.of(
                        "status", "CANCELLED",
                        "cancellationNote", saved.getCancellationNote()
                )
        );

        return WorkOrderDto.from(saved);
    }
}
