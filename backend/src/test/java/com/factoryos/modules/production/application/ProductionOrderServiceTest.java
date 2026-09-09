package com.factoryos.modules.production.application;

import com.factoryos.common.exception.AppException;
import com.factoryos.modules.audit.application.AuditRecordingService;
import com.factoryos.modules.machine.domain.Machine;
import com.factoryos.modules.machine.domain.MachineStatus;
import com.factoryos.modules.machine.repository.MachineRepository;
import com.factoryos.modules.production.domain.ProductionOrder;
import com.factoryos.modules.production.domain.ProductionOrderStatus;
import com.factoryos.modules.production.dto.CreateProductionOrderRequest;
import com.factoryos.modules.production.dto.ProductionOrderDto;
import com.factoryos.modules.production.dto.TransitionOrderStatusRequest;
import com.factoryos.modules.production.dto.UpdateProductionProgressRequest;
import com.factoryos.modules.production.repository.ProductionOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductionOrderServiceTest {

    @Mock
    private ProductionOrderRepository productionOrderRepository;

    @Mock
    private MachineRepository machineRepository;

    @Mock
    private AuditRecordingService auditRecordingService;

    private ProductionOrderService productionOrderService;

    @BeforeEach
    void setUp() {
        productionOrderService = new ProductionOrderService(
                productionOrderRepository,
                machineRepository,
                auditRecordingService
        );
    }

    @Test
    void createProductionOrder_Success_InitializesInDraft() {
        UUID machineId = UUID.randomUUID();
        Machine machine = new Machine();
        machine.setId(machineId);
        machine.setName("CNC Line 1");

        when(productionOrderRepository.existsByOrderNumberAndIsDeletedFalse("PO-2026-001")).thenReturn(false);
        when(machineRepository.findByIdAndIsDeletedFalse(machineId)).thenReturn(Optional.of(machine));

        ProductionOrder savedOrder = new ProductionOrder();
        savedOrder.setId(UUID.randomUUID());
        savedOrder.setOrderNumber("PO-2026-001");
        savedOrder.setMachine(machine);
        savedOrder.setProductCode("SKU-99");
        savedOrder.setPlannedQuantity(500);
        savedOrder.setStatus(ProductionOrderStatus.DRAFT);
        when(productionOrderRepository.save(any(ProductionOrder.class))).thenReturn(savedOrder);

        CreateProductionOrderRequest request = new CreateProductionOrderRequest();
        request.setOrderNumber("PO-2026-001");
        request.setMachineId(machineId);
        request.setProductCode("SKU-99");
        request.setPlannedQuantity(500);

        ProductionOrderDto result = productionOrderService.createProductionOrder(request, null);

        assertNotNull(result);
        assertEquals(ProductionOrderStatus.DRAFT, result.getStatus());
        assertEquals(500, result.getPlannedQuantity());
        verify(auditRecordingService).record(isNull(), eq("PRODUCTION_ORDER_CREATED"), eq("ProductionOrder"), eq(savedOrder.getId()), isNull(), anyMap());
    }

    @Test
    void updateProgress_Success_UpdatesGoodAndScrapCounts() {
        UUID orderId = UUID.randomUUID();
        Machine machine = new Machine();
        machine.setId(UUID.randomUUID());
        machine.setName("CNC Line 1");

        ProductionOrder order = new ProductionOrder();
        order.setId(orderId);
        order.setMachine(machine);
        order.setStatus(ProductionOrderStatus.IN_PROGRESS);
        order.setGoodQuantity(100);
        order.setScrapQuantity(2);
        order.setVersion(0L);

        when(productionOrderRepository.findByIdAndIsDeletedFalse(orderId)).thenReturn(Optional.of(order));
        when(productionOrderRepository.save(any(ProductionOrder.class))).thenReturn(order);

        UpdateProductionProgressRequest request = new UpdateProductionProgressRequest();
        request.setGoodQuantity(150);
        request.setScrapQuantity(4);
        request.setExpectedVersion(0L);

        ProductionOrderDto result = productionOrderService.updateProgress(orderId, request, null);

        assertNotNull(result);
        assertEquals(150, order.getGoodQuantity());
        assertEquals(4, order.getScrapQuantity());
        verify(auditRecordingService).record(isNull(), eq("PRODUCTION_PROGRESS_UPDATED"), eq("ProductionOrder"), eq(orderId), anyMap(), anyMap());
    }

    @Test
    void transitionStatus_ToInProgress_SetsMachineToRunning() {
        UUID orderId = UUID.randomUUID();
        Machine machine = new Machine();
        machine.setId(UUID.randomUUID());
        machine.setName("CNC Line 1");
        machine.setStatus(MachineStatus.IDLE);

        ProductionOrder order = new ProductionOrder();
        order.setId(orderId);
        order.setMachine(machine);
        order.setStatus(ProductionOrderStatus.RELEASED);
        order.setVersion(0L);

        when(productionOrderRepository.findByIdAndIsDeletedFalse(orderId)).thenReturn(Optional.of(order));
        when(productionOrderRepository.findByMachineAndStatus(machine, ProductionOrderStatus.IN_PROGRESS)).thenReturn(Optional.empty());
        when(productionOrderRepository.save(any(ProductionOrder.class))).thenReturn(order);

        TransitionOrderStatusRequest request = new TransitionOrderStatusRequest();
        request.setTargetStatus(ProductionOrderStatus.IN_PROGRESS);
        request.setExpectedVersion(0L);

        ProductionOrderDto result = productionOrderService.transitionStatus(orderId, request, null);

        assertNotNull(result);
        assertEquals(ProductionOrderStatus.IN_PROGRESS, order.getStatus());
        assertEquals(MachineStatus.RUNNING, machine.getStatus());
        verify(machineRepository).save(machine);
        verify(auditRecordingService).record(isNull(), eq("PRODUCTION_STATUS_CHANGED"), eq("ProductionOrder"), eq(orderId), anyMap(), anyMap());
    }

    @Test
    void transitionStatus_ToCompleted_SetsMachineToIdle() {
        UUID orderId = UUID.randomUUID();
        Machine machine = new Machine();
        machine.setId(UUID.randomUUID());
        machine.setName("CNC Line 1");
        machine.setStatus(MachineStatus.RUNNING);

        ProductionOrder order = new ProductionOrder();
        order.setId(orderId);
        order.setMachine(machine);
        order.setStatus(ProductionOrderStatus.IN_PROGRESS);
        order.setVersion(1L);

        when(productionOrderRepository.findByIdAndIsDeletedFalse(orderId)).thenReturn(Optional.of(order));
        when(productionOrderRepository.save(any(ProductionOrder.class))).thenReturn(order);

        TransitionOrderStatusRequest request = new TransitionOrderStatusRequest();
        request.setTargetStatus(ProductionOrderStatus.COMPLETED);
        request.setClosureNote("Batch inspection passed.");
        request.setExpectedVersion(1L);

        ProductionOrderDto result = productionOrderService.transitionStatus(orderId, request, null);

        assertNotNull(result);
        assertEquals(ProductionOrderStatus.COMPLETED, order.getStatus());
        assertEquals(MachineStatus.IDLE, machine.getStatus());
        assertNotNull(order.getCompletedAt());
        verify(machineRepository).save(machine);
    }
}
