package com.factoryos.modules.production.application;

import com.factoryos.common.dto.PagedResponse;
import com.factoryos.common.exception.AppException;
import com.factoryos.modules.audit.application.AuditRecordingService;
import com.factoryos.modules.auth.domain.User;
import com.factoryos.modules.machine.domain.Machine;
import com.factoryos.modules.machine.domain.MachineStatus;
import com.factoryos.modules.machine.repository.MachineRepository;
import com.factoryos.modules.production.domain.ProductionOrder;
import com.factoryos.modules.production.domain.ProductionOrderStatus;
import com.factoryos.modules.production.dto.*;
import com.factoryos.modules.production.repository.ProductionOrderRepository;
import com.factoryos.modules.tenant.context.TenantContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProductionOrderService {

    private final ProductionOrderRepository productionOrderRepository;
    private final MachineRepository machineRepository;
    private final AuditRecordingService auditRecordingService;

    public ProductionOrderService(
            ProductionOrderRepository productionOrderRepository,
            MachineRepository machineRepository,
            AuditRecordingService auditRecordingService
    ) {
        this.productionOrderRepository = productionOrderRepository;
        this.machineRepository = machineRepository;
        this.auditRecordingService = auditRecordingService;
    }

    public PagedResponse<ProductionOrderDto> getProductionOrders(
            UUID machineId,
            ProductionOrderStatus status,
            String search,
            Pageable pageable
    ) {
        UUID plantId = TenantContextHolder.getCurrentPlantId();
        Page<ProductionOrder> page;
        if (plantId != null && !TenantContextHolder.isGlobalAdmin()) {
            page = productionOrderRepository.searchOrdersWithPlant(plantId, machineId, status, search, pageable);
        } else if (plantId != null) {
            page = productionOrderRepository.searchOrdersWithPlant(plantId, machineId, status, search, pageable);
        } else {
            page = productionOrderRepository.searchOrders(machineId, status, search, pageable);
        }
        return PagedResponse.from(page.map(ProductionOrderDto::from));
    }

    public ProductionOrderDto getProductionOrderById(UUID id) {
        ProductionOrder order = productionOrderRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> AppException.notFound("Production order not found with ID: " + id));

        validatePlantAccess(order);

        return ProductionOrderDto.from(order);
    }

    public Optional<ProductionOrderDto> getActiveOrderForMachine(UUID machineId) {
        return productionOrderRepository.findByMachineIdAndStatus(machineId, ProductionOrderStatus.IN_PROGRESS)
                .map(order -> {
                    validatePlantAccess(order);
                    return ProductionOrderDto.from(order);
                });
    }

    @Transactional
    public ProductionOrderDto createProductionOrder(CreateProductionOrderRequest request, User actor) {
        String orderNumber = request.getOrderNumber().trim().toUpperCase();
        if (productionOrderRepository.existsByOrderNumberAndIsDeletedFalse(orderNumber)) {
            throw AppException.conflict("DUPLICATE_ORDER_NUMBER", "An order with number '" + orderNumber + "' already exists");
        }

        Machine machine = machineRepository.findByIdAndIsDeletedFalse(request.getMachineId())
                .orElseThrow(() -> AppException.notFound("Machine not found with ID: " + request.getMachineId()));

        if (machine.getPlant() != null && !TenantContextHolder.isGlobalAdmin()) {
            UUID currentPlantId = TenantContextHolder.getCurrentPlantId();
            if (currentPlantId != null && !currentPlantId.equals(machine.getPlant().getId())) {
                throw AppException.forbidden("Cross-tenant violation: Cannot create order on machine at plant " + machine.getPlant().getCode());
            }
        }

        ProductionOrder order = new ProductionOrder();
        order.setOrderNumber(orderNumber);
        order.setMachine(machine);
        order.setPlant(machine.getPlant());
        order.setProductCode(request.getProductCode().trim());
        order.setProductDescription(request.getProductDescription() != null ? request.getProductDescription().trim() : null);
        order.setPlannedQuantity(request.getPlannedQuantity());
        order.setGoodQuantity(0);
        order.setScrapQuantity(0);
        order.setStatus(ProductionOrderStatus.DRAFT);
        order.setCreatedBy(actor != null ? actor.getId() : null);
        order.setUpdatedBy(actor != null ? actor.getId() : null);

        ProductionOrder saved = productionOrderRepository.save(order);

        auditRecordingService.record(
                actor != null ? actor.getId() : null,
                "PRODUCTION_ORDER_CREATED",
                "ProductionOrder",
                saved.getId(),
                null,
                Map.of(
                        "orderNumber", saved.getOrderNumber(),
                        "machineId", machine.getId().toString(),
                        "productCode", saved.getProductCode(),
                        "plannedQuantity", saved.getPlannedQuantity()
                )
        );

        return ProductionOrderDto.from(saved);
    }

    @Transactional
    public ProductionOrderDto updateProductionOrder(UUID id, UpdateProductionOrderRequest request, User actor) {
        ProductionOrder order = productionOrderRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> AppException.notFound("Production order not found with ID: " + id));

        validatePlantAccess(order);

        if (request.getExpectedVersion() == null || !request.getExpectedVersion().equals(order.getVersion())) {
            throw AppException.versionConflict("Production order modified by another transaction. Reload and retry.");
        }

        if (order.getStatus() == ProductionOrderStatus.COMPLETED || order.getStatus() == ProductionOrderStatus.CANCELLED) {
            throw AppException.badRequest("Cannot edit completed or cancelled production orders.");
        }

        Map<String, Object> beforeState = Map.of(
                "productCode", order.getProductCode(),
                "plannedQuantity", order.getPlannedQuantity()
        );

        if (request.getProductCode() != null && !request.getProductCode().isBlank()) {
            order.setProductCode(request.getProductCode().trim());
        }
        if (request.getProductDescription() != null) {
            order.setProductDescription(request.getProductDescription().trim());
        }
        if (request.getPlannedQuantity() != null) {
            order.setPlannedQuantity(request.getPlannedQuantity());
        }

        order.setUpdatedBy(actor != null ? actor.getId() : null);
        order.setUpdatedAt(Instant.now());

        ProductionOrder saved = productionOrderRepository.save(order);

        auditRecordingService.record(
                actor != null ? actor.getId() : null,
                "PRODUCTION_ORDER_UPDATED",
                "ProductionOrder",
                saved.getId(),
                beforeState,
                Map.of(
                        "productCode", saved.getProductCode(),
                        "plannedQuantity", saved.getPlannedQuantity()
                )
        );

        return ProductionOrderDto.from(saved);
    }

    @Transactional
    public ProductionOrderDto updateProgress(UUID id, UpdateProductionProgressRequest request, User actor) {
        ProductionOrder order = productionOrderRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> AppException.notFound("Production order not found with ID: " + id));

        validatePlantAccess(order);

        if (request.getExpectedVersion() == null || !request.getExpectedVersion().equals(order.getVersion())) {
            throw AppException.versionConflict("Production order progress updated by another operator. Reload and retry.");
        }

        if (order.getStatus() != ProductionOrderStatus.IN_PROGRESS && order.getStatus() != ProductionOrderStatus.RELEASED) {
            throw AppException.badRequest("Progress can only be updated when order is RELEASED or IN_PROGRESS.");
        }

        Map<String, Object> beforeState = Map.of(
                "goodQuantity", order.getGoodQuantity(),
                "scrapQuantity", order.getScrapQuantity()
        );

        order.setGoodQuantity(request.getGoodQuantity());
        order.setScrapQuantity(request.getScrapQuantity());
        order.setUpdatedBy(actor != null ? actor.getId() : null);
        order.setUpdatedAt(Instant.now());

        ProductionOrder saved = productionOrderRepository.save(order);

        auditRecordingService.record(
                actor != null ? actor.getId() : null,
                "PRODUCTION_PROGRESS_UPDATED",
                "ProductionOrder",
                saved.getId(),
                beforeState,
                Map.of(
                        "goodQuantity", saved.getGoodQuantity(),
                        "scrapQuantity", saved.getScrapQuantity()
                )
        );

        return ProductionOrderDto.from(saved);
    }

    @Transactional
    public ProductionOrderDto transitionStatus(UUID id, TransitionOrderStatusRequest request, User actor) {
        ProductionOrder order = productionOrderRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> AppException.notFound("Production order not found with ID: " + id));

        validatePlantAccess(order);

        if (request.getExpectedVersion() == null || !request.getExpectedVersion().equals(order.getVersion())) {
            throw AppException.versionConflict("Production order status changed by another operator. Reload and retry.");
        }

        ProductionOrderStatus currentStatus = order.getStatus();
        ProductionOrderStatus targetStatus = request.getTargetStatus();

        if (currentStatus == targetStatus) {
            return ProductionOrderDto.from(order);
        }

        // Validate state machine transitions
        validateTransition(currentStatus, targetStatus);

        Machine machine = order.getMachine();

        if (targetStatus == ProductionOrderStatus.IN_PROGRESS) {
            // Check no other order is IN_PROGRESS on this machine
            Optional<ProductionOrder> existingActive = productionOrderRepository.findByMachineAndStatus(machine, ProductionOrderStatus.IN_PROGRESS);
            if (existingActive.isPresent() && !existingActive.get().getId().equals(order.getId())) {
                throw AppException.conflict("ACTIVE_ORDER_EXISTS", "Machine " + machine.getName() + " already has active order " + existingActive.get().getOrderNumber());
            }

            order.setStartedAt(Instant.now());
            if (machine.getStatus() == MachineStatus.IDLE) {
                machine.setStatus(MachineStatus.RUNNING);
                machine.setUpdatedBy(actor != null ? actor.getId() : null);
                machine.setUpdatedAt(Instant.now());
                machineRepository.save(machine);
            }
        } else if (targetStatus == ProductionOrderStatus.COMPLETED) {
            order.setCompletedAt(Instant.now());
            order.setClosedAt(Instant.now());
            if (request.getClosureNote() != null) {
                order.setClosureNote(request.getClosureNote().trim());
            }
            if (machine.getStatus() == MachineStatus.RUNNING) {
                machine.setStatus(MachineStatus.IDLE);
                machine.setUpdatedBy(actor != null ? actor.getId() : null);
                machine.setUpdatedAt(Instant.now());
                machineRepository.save(machine);
            }
        } else if (targetStatus == ProductionOrderStatus.CANCELLED) {
            order.setClosedAt(Instant.now());
            if (request.getClosureNote() != null) {
                order.setClosureNote(request.getClosureNote().trim());
            }
            if (currentStatus == ProductionOrderStatus.IN_PROGRESS && machine.getStatus() == MachineStatus.RUNNING) {
                machine.setStatus(MachineStatus.IDLE);
                machine.setUpdatedBy(actor != null ? actor.getId() : null);
                machine.setUpdatedAt(Instant.now());
                machineRepository.save(machine);
            }
        }

        order.setStatus(targetStatus);
        order.setUpdatedBy(actor != null ? actor.getId() : null);
        order.setUpdatedAt(Instant.now());

        ProductionOrder saved = productionOrderRepository.save(order);

        auditRecordingService.record(
                actor != null ? actor.getId() : null,
                "PRODUCTION_STATUS_CHANGED",
                "ProductionOrder",
                saved.getId(),
                Map.of("fromStatus", currentStatus.name()),
                Map.of("toStatus", targetStatus.name())
        );

        return ProductionOrderDto.from(saved);
    }

    private void validatePlantAccess(ProductionOrder order) {
        if (order.getPlant() != null && !TenantContextHolder.isGlobalAdmin()) {
            UUID currentPlantId = TenantContextHolder.getCurrentPlantId();
            if (currentPlantId != null && !currentPlantId.equals(order.getPlant().getId())) {
                throw AppException.forbidden("Cross-tenant access violation: Production order belongs to plant " + order.getPlant().getCode());
            }
        }
    }

    private void validateTransition(ProductionOrderStatus from, ProductionOrderStatus to) {
        if (from == ProductionOrderStatus.COMPLETED || from == ProductionOrderStatus.CANCELLED) {
            throw AppException.badRequest("Cannot transition order from terminal status: " + from);
        }

        switch (from) {
            case DRAFT:
                if (to != ProductionOrderStatus.RELEASED && to != ProductionOrderStatus.CANCELLED) {
                    throw AppException.badRequest("Invalid transition from DRAFT to " + to + ". Must be RELEASED or CANCELLED.");
                }
                break;
            case RELEASED:
                if (to != ProductionOrderStatus.IN_PROGRESS && to != ProductionOrderStatus.CANCELLED) {
                    throw AppException.badRequest("Invalid transition from RELEASED to " + to + ". Must be IN_PROGRESS or CANCELLED.");
                }
                break;
            case IN_PROGRESS:
                if (to != ProductionOrderStatus.COMPLETED && to != ProductionOrderStatus.CANCELLED) {
                    throw AppException.badRequest("Invalid transition from IN_PROGRESS to " + to + ". Must be COMPLETED or CANCELLED.");
                }
                break;
        }
    }
}
