package com.factoryos.modules.operations.application;

import com.factoryos.common.exception.AppException;
import com.factoryos.modules.audit.application.AuditRecordingService;
import com.factoryos.modules.auth.domain.User;
import com.factoryos.modules.downtime.application.DowntimeService;
import com.factoryos.modules.downtime.domain.DowntimeEvent;
import com.factoryos.modules.downtime.dto.CreateDowntimeRequest;
import com.factoryos.modules.downtime.dto.DowntimeEventDto;
import com.factoryos.modules.downtime.dto.ResolveDowntimeRequest;
import com.factoryos.modules.downtime.repository.DowntimeEventRepository;
import com.factoryos.modules.machine.domain.Machine;
import com.factoryos.modules.machine.domain.MachineStatus;
import com.factoryos.modules.machine.repository.MachineRepository;
import com.factoryos.modules.maintenance.application.MaintenanceService;
import com.factoryos.modules.maintenance.domain.MaintenancePriority;
import com.factoryos.modules.maintenance.dto.CreateWorkOrderRequest;
import com.factoryos.modules.maintenance.dto.WorkOrderDto;
import com.factoryos.modules.operations.dto.BreakdownReportRequest;
import com.factoryos.modules.operations.dto.ResolveBreakdownRequest;
import com.factoryos.modules.production.application.ProductionOrderService;
import com.factoryos.modules.production.domain.ProductionOrder;
import com.factoryos.modules.production.domain.ProductionOrderStatus;
import com.factoryos.modules.production.dto.ProductionOrderDto;
import com.factoryos.modules.production.dto.TransitionOrderStatusRequest;
import com.factoryos.modules.production.repository.ProductionOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class OperationsCoordinator {

    private final MachineRepository machineRepository;
    private final DowntimeService downtimeService;
    private final DowntimeEventRepository downtimeEventRepository;
    private final ProductionOrderService productionOrderService;
    private final ProductionOrderRepository productionOrderRepository;
    private final MaintenanceService maintenanceService;
    private final AuditRecordingService auditRecordingService;

    public OperationsCoordinator(
            MachineRepository machineRepository,
            DowntimeService downtimeService,
            DowntimeEventRepository downtimeEventRepository,
            ProductionOrderService productionOrderService,
            ProductionOrderRepository productionOrderRepository,
            MaintenanceService maintenanceService,
            AuditRecordingService auditRecordingService
    ) {
        this.machineRepository = machineRepository;
        this.downtimeService = downtimeService;
        this.downtimeEventRepository = downtimeEventRepository;
        this.productionOrderService = productionOrderService;
        this.productionOrderRepository = productionOrderRepository;
        this.maintenanceService = maintenanceService;
        this.auditRecordingService = auditRecordingService;
    }

    @Transactional
    public DowntimeEventDto reportBreakdown(BreakdownReportRequest request, User actor) {
        Machine machine = machineRepository.findByIdAndIsDeletedFalse(request.getMachineId())
                .orElseThrow(() -> AppException.notFound("Machine not found with ID: " + request.getMachineId()));

        CreateDowntimeRequest dtRequest = new CreateDowntimeRequest();
        dtRequest.setMachineId(machine.getId());
        dtRequest.setReasonCode(request.getReasonCode());
        dtRequest.setDescription(request.getDescription());
        dtRequest.setStartTime(Instant.now());

        DowntimeEventDto downtimeDto = downtimeService.createDowntimeEvent(dtRequest, actor);

        // If requested, generate a maintenance work order automatically
        if (request.isCreateWorkOrder()) {
            CreateWorkOrderRequest woRequest = new CreateWorkOrderRequest();
            woRequest.setMachineId(machine.getId());
            woRequest.setDowntimeEventId(downtimeDto.getId());
            woRequest.setWorkOrderNumber("WO-AUTO-" + System.currentTimeMillis() % 100000);
            woRequest.setTitle(request.getWorkOrderTitle() != null && !request.getWorkOrderTitle().isBlank() 
                    ? request.getWorkOrderTitle() 
                    : "Emergency Repair for " + machine.getName() + " (" + request.getReasonCode() + ")");
            woRequest.setDescription(request.getDescription() != null ? request.getDescription() : "Automated work order from breakdown report.");
            woRequest.setPriority(MaintenancePriority.HIGH);
            maintenanceService.createWorkOrder(woRequest, actor);
        }

        auditRecordingService.record(
                actor != null ? actor.getId() : null,
                "OPERATIONS_BREAKDOWN_REPORTED",
                "Machine",
                machine.getId(),
                null,
                Map.of(
                        "machineId", machine.getId().toString(),
                        "downtimeEventId", downtimeDto.getId().toString(),
                        "reasonCode", request.getReasonCode().name()
                )
        );

        return downtimeDto;
    }

    @Transactional
    public DowntimeEventDto resolveBreakdown(UUID machineId, ResolveBreakdownRequest request, User actor) {
        Machine machine = machineRepository.findByIdAndIsDeletedFalse(machineId)
                .orElseThrow(() -> AppException.notFound("Machine not found with ID: " + machineId));

        ResolveDowntimeRequest dtResolve = new ResolveDowntimeRequest();
        dtResolve.setResolutionNote(request.getResolutionNote());
        dtResolve.setExpectedVersion(request.getExpectedVersion());
        dtResolve.setEndTime(Instant.now());

        DowntimeEventDto resolved = downtimeService.resolveDowntimeEvent(request.getDowntimeEventId(), dtResolve, actor);

        // Adjust target machine status if requested (e.g. IDLE or RUNNING)
        if (request.getTargetMachineStatus() != null && request.getTargetMachineStatus() != machine.getStatus()) {
            machine.setStatus(request.getTargetMachineStatus());
            machine.setUpdatedBy(actor != null ? actor.getId() : null);
            machine.setUpdatedAt(Instant.now());
            machineRepository.save(machine);
        }

        auditRecordingService.record(
                actor != null ? actor.getId() : null,
                "OPERATIONS_BREAKDOWN_RESOLVED",
                "Machine",
                machine.getId(),
                null,
                Map.of(
                        "machineId", machine.getId().toString(),
                        "downtimeEventId", resolved.getId().toString(),
                        "targetMachineStatus", machine.getStatus().name()
                )
        );

        return resolved;
    }

    @Transactional
    public ProductionOrderDto startProductionRun(UUID orderId, Long expectedVersion, User actor) {
        TransitionOrderStatusRequest transition = new TransitionOrderStatusRequest();
        transition.setTargetStatus(ProductionOrderStatus.IN_PROGRESS);
        transition.setExpectedVersion(expectedVersion);
        return productionOrderService.transitionStatus(orderId, transition, actor);
    }
}
